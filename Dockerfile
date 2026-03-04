# --- Stage 1: Build the application ---
# Use the full JDK image for building
FROM bellsoft/liberica-openjdk-alpine:25 AS build
WORKDIR /home/app

# Copy Maven Wrapper and pom.xml first to cache dependencies
COPY .mvn .mvn
COPY mvnw pom.xml lombok.config ./
RUN chmod +x mvnw

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy Ebean agent (assumed to be in the project structure)
COPY src/main/jib/ebean-agent-17.3.0.jar ./ebean-agent.jar

# Copy source and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Download Standard JDK (to get jmods for jlink) ---
# We use Alpine as a base to download the musl-based JDK
FROM bellsoft/liberica-openjdk-alpine:25 AS jdk-downloader
ARG TARGETARCH
WORKDIR /opt/jdk-download

RUN ARCH=$TARGETARCH && \
    if [ "$ARCH" = "x86_64" ]; then \
        URL="https://download.bell-sw.com/java/25.0.2+12/bellsoft-jdk25.0.2+12-linux-x64-musl.tar.gz"; \
    elif [ "$ARCH" = "arm64" ]; then \
        URL="https://download.bell-sw.com/java/25.0.2+12/bellsoft-jdk25.0.2+12-linux-aarch64-musl.tar.gz"; \
    else \
        echo "Unsupported architecture: $ARCH" && exit 1; \
    fi && \
    wget -q $URL -O jdk.tar.gz && \
    tar -xzf jdk.tar.gz && \
    rm jdk.tar.gz && \
    mv jdk-25.0.2* jdk-full

# --- Stage 3: Create a minimal JRE with jlink ---
FROM bellsoft/liberica-openjdk-alpine:25 AS jre-builder
WORKDIR /opt/jre-build

# Install binutils for objcopy (required by --strip-debug)
RUN apk add --no-cache binutils

# Copy the full JDK from the downloader stage
COPY --from=jdk-downloader /opt/jdk-download/jdk-full ./jdk-full

# Copy the built JAR and its dependencies to analyze required modules
COPY --from=build /home/app/target/*.jar ./app.jar
COPY --from=build /home/app/target/lib ./lib

# Determine required modules using jdeps
RUN ./jdk-full/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --recursive \
    --multi-release 25 \
    --class-path 'lib/*' \
    app.jar > modules.txt

# Create custom JRE using the jmods from the full JDK
RUN ./jdk-full/bin/jlink \
    --module-path ./jdk-full/jmods \
    --add-modules $(cat modules.txt),jdk.crypto.ec,jdk.management \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress zip-6 \
    --output /opt/jre

# --- Stage 4: Generate AppCDS archive ---
FROM bellsoft/alpaquita-linux-base:musl AS cds-builder
WORKDIR /opt/app

COPY --from=jre-builder /opt/jre /opt/jre
COPY --from=build /home/app/target/*.jar ./app.jar
COPY --from=build /home/app/target/lib ./lib
COPY --from=build /home/app/ebean-agent.jar ./ebean-agent.jar

ENV PATH="/opt/jre/bin:$PATH"

# Create a dynamic archive for the application
RUN java -XX:+UnlockDiagnosticVMOptions \
    -XX:+AllowArchivingWithJavaAgent \
    -XX:+UseZGC \
    -javaagent:ebean-agent.jar \
    --add-modules java.instrument \
    -Xshare:dump \
    -XX:SharedArchiveFile=app-cds.jsa \
    -jar app.jar || true

# --- Stage 5: Final runtime image ---
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
  CMD wget --no-check-certificate --quiet --tries=1 --spider http://localhost:8080/v1/genres || exit 1

# Performance Tuning and Runtime Configuration
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
