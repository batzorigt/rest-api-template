# --- Stage 1: Build the application ---
FROM bellsoft/liberica-openjdk-alpine:25 AS build
WORKDIR /home/app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml and download dependencies
COPY pom.xml .
COPY lombok.config .
RUN mvn dependency:go-offline -B

# Download ebean-agent for runtime enhancement
#RUN wget -q https://repo1.maven.org/maven2/io/ebean/ebean-agent/17.3.0/ebean-agent-17.3.0.jar -O ebean-agent.jar
COPY src/main/jib/ebean-agent-17.3.0.jar ./ebean-agent.jar

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Create a minimal JRE with jlink ---
FROM bellsoft/liberica-openjdk-alpine:25 AS jre-builder
WORKDIR /opt/jre-build

# Install binutils for objcopy (required by --strip-debug)
RUN apk add --no-cache binutils

# Copy the built JAR to analyze dependencies
COPY --from=build /home/app/target/*.jar ./app.jar
COPY --from=build /home/app/target/lib ./lib

# Download Standard JDK (not Lite) to get jmods for jlink
# Detect architecture and download the right one
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then \
        URL="https://download.bell-sw.com/java/25.0.2+12/bellsoft-jdk25.0.2+12-linux-amd64-musl.tar.gz"; \
    else \
        URL="https://download.bell-sw.com/java/25.0.2+12/bellsoft-jdk25.0.2+12-linux-aarch64-musl.tar.gz"; \
    fi && \
    wget -q $URL -O jdk.tar.gz && \
    tar -xzf jdk.tar.gz && \
    rm jdk.tar.gz && \
    mv jdk-25.0.2* jdk-full

# Determine required modules using jdeps
RUN ./jdk-full/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --recursive \
    --multi-release 25 \
    --class-path 'lib/*' \
    app.jar > modules.txt

# Create custom JRE using the jmods from the full JDK
# --compress zip-6 is the new syntax for jlink compression
RUN ./jdk-full/bin/jlink \
    --module-path ./jdk-full/jmods \
    --add-modules $(cat modules.txt),jdk.crypto.ec,jdk.management \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress zip-6 \
    --output /opt/jre

# --- Stage 3: Generate AppCDS archive ---
FROM bellsoft/alpaquita-linux-base:musl AS cds-builder
WORKDIR /opt/app

COPY --from=jre-builder /opt/jre /opt/jre
COPY --from=build /home/app/target/*.jar ./app.jar
COPY --from=build /home/app/target/lib ./lib
COPY --from=build /home/app/ebean-agent.jar ./ebean-agent.jar

ENV PATH="/opt/jre/bin:$PATH"

# 1. Create a dynamic archive for the application
# IMPORTANT: Use the same flags as runtime (especially -XX:+UseZGC and modules added by agents)
RUN java -XX:+UnlockDiagnosticVMOptions \
    -XX:+AllowArchivingWithJavaAgent \
    -XX:+UseZGC \
    -javaagent:ebean-agent.jar \
    --add-modules java.instrument \
    -Xshare:dump \
    -XX:SharedArchiveFile=app-cds.jsa \
    -jar app.jar || true

# --- Stage 4: Final runtime image ---
FROM bellsoft/alpaquita-linux-base:musl
WORKDIR /opt/app

LABEL org.opencontainers.image.title="Rest API Template" \
      org.opencontainers.image.authors="Batzorigt Rentsen" \
      org.opencontainers.image.version="1.0.0"

# Security: Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JRE and application files
COPY --from=jre-builder /opt/jre /opt/jre
COPY --from=build /home/app/target/*.jar ./app.jar
COPY --from=build /home/app/target/lib ./lib
COPY --from=build /home/app/ebean-agent.jar ./ebean-agent.jar
COPY --from=cds-builder /opt/app/app-cds.jsa ./app-cds.jsa

ENV PATH="/opt/jre/bin:$PATH"
ENV JAVA_HOME="/opt/jre"

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -no-check-certificate --quiet --tries=1 --spider http://localhost:8080/v1/genres || exit 1

# Virtual Threads and Performance Tuning
# -XX:+UseZGC: Low latency, generational by default in Java 25
# -XX:MaxRAMPercentage: Better container awareness
# -Xshare:on: Use CDS for faster startup
ENTRYPOINT ["java", \
    "-javaagent:ebean-agent.jar", \
    "-Dlog4j2.formatMsgNoLookups=true", \
    "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector", \
    "-XX:+UnlockDiagnosticVMOptions", \
    "-XX:+AllowArchivingWithJavaAgent", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Xshare:on", \
    "-XX:SharedArchiveFile=app-cds.jsa", \
    "-Djdk.virtualThreadScheduler.parallelism=2", \
    "-jar", \
    "app.jar"]

USER appuser
EXPOSE 8080
