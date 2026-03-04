# --- Global Arguments ---
ARG JAVA_VERSION=25.0.2+12
ARG EBEAN_AGENT_VERSION=17.3.0
ARG APP_VERSION=1.0.0

# --- Stage 1: Build the application ---
FROM bellsoft/liberica-openjdk-alpine:25 AS build
ARG EBEAN_AGENT_VERSION
WORKDIR /home/app

# Copy Maven Wrapper and pom.xml first to cache dependencies
COPY .mvn .mvn
COPY mvnw pom.xml lombok.config ./
RUN chmod +x mvnw

# Download dependencies (using BuildKit cache mount for faster rebuilds)
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -B

# Download ebean-agent for runtime enhancement (Automated)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:copy -Dartifact=io.ebean:ebean-agent:${EBEAN_AGENT_VERSION}:jar -DoutputDirectory=/home/app -Dmdep.useBaseVersion=true && \
    mv /home/app/ebean-agent-${EBEAN_AGENT_VERSION}.jar /home/app/ebean-agent.jar

# Copy source and build the application
COPY src ./src
# -T 1C: Uses 1 thread per core to speed up the build
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests -B -T 1C

# --- Stage 2: Download Standard JDK (to get jmods for jlink) ---
FROM bellsoft/liberica-openjdk-alpine:25 AS jdk-downloader
ARG TARGETARCH
ARG JAVA_VERSION
WORKDIR /opt/jdk-download

RUN case "${TARGETARCH}" in \
    "amd64"|"x86_64") ARCH="x64" ;; \
    "arm64"|"aarch64") ARCH="aarch64" ;; \
    *) echo "Unsupported architecture: ${TARGETARCH}"; exit 1 ;; \
    esac && \
    URL="https://download.bell-sw.com/java/${JAVA_VERSION}/bellsoft-jdk${JAVA_VERSION}-linux-${ARCH}-musl.tar.gz" && \
    wget -q $URL -O jdk.tar.gz && \
    tar -xzf jdk.tar.gz && \
    rm jdk.tar.gz && \
    mv jdk-* jdk-full

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
    --add-modules $(cat modules.txt),jdk.management,jdk.crypto.ec \
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
    -Djava.awt.headless=true \
    -Xshare:dump \
    -XX:SharedArchiveFile=app-cds.jsa \
    -jar app.jar || true

# --- Stage 5: Final runtime image ---
FROM bellsoft/alpaquita-linux-base:musl
ARG APP_VERSION
WORKDIR /opt/app

LABEL org.opencontainers.image.title="Rest API Template" \
      org.opencontainers.image.description="High-performance Java REST API using Javalin and Ebean" \
      org.opencontainers.image.authors="Batzorigt Rentsen" \
      org.opencontainers.image.version="${APP_VERSION}"

# Security: Create a non-root user and set permissions
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /opt/app

# Copy JRE and application files
COPY --from=jre-builder --chown=appuser:appgroup /opt/jre /opt/jre
COPY --from=build --chown=appuser:appgroup /home/app/target/*.jar ./app.jar
COPY --from=build --chown=appuser:appgroup /home/app/target/lib ./lib
COPY --from=build --chown=appuser:appgroup /home/app/ebean-agent.jar ./ebean-agent.jar
COPY --from=cds-builder --chown=appuser:appgroup /opt/app/app-cds.jsa ./app-cds.jsa

ENV PATH="/opt/jre/bin:$PATH"
ENV JAVA_HOME="/opt/jre"
ENV JAVA_OPTS="-XX:+UnlockDiagnosticVMOptions -XX:+AllowArchivingWithJavaAgent -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Xshare:on -XX:SharedArchiveFile=app-cds.jsa -Djdk.virtualThreadScheduler.parallelism=2"

# Improved Healthcheck: Accept 404 as "Up" for this specific endpoint if DB is empty
HEALTHCHECK --interval=30s --timeout=1s --start-period=10s --retries=3 \
  CMD wget --no-check-certificate --quiet --tries=1 --spider http://localhost:8080/v1/genres || \
      wget --no-check-certificate --quiet --tries=1 --spider --server-response http://localhost:8080/v1/genres 2>&1 | grep -q "404 Not Found" || exit 1

# Performance Tuning and Runtime Configuration
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:ebean-agent.jar -Dlog4j2.formatMsgNoLookups=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar app.jar"]

USER appuser
EXPOSE 8080
