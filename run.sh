#!/bin/bash

# Project configuration
JAR_FILE="target/rest-api-template-1.0.0.jar"
EBEAN_VERSION="$(sed -n 's:.*<ebean.version>\(.*\)</ebean.version>.*:\1:p' pom.xml | head -n 1)"
AGENT_FILE="src/main/jib/ebean-agent-${EBEAN_VERSION}.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found. Please run 'mvn clean package -DskipTests' first."
    exit 1
fi

if [ -z "$EBEAN_VERSION" ]; then
    echo "Error: Could not read ebean.version from pom.xml."
    exit 1
fi

if [ ! -f "$AGENT_FILE" ]; then
    echo "Error: $AGENT_FILE not found. Please sync the agent jar with pom.xml ebean.version."
    exit 1
fi

# JVM Options (Matched with Dockerfile)
JAVA_OPTS=" \
    -javaagent:$AGENT_FILE \
    -Dlog4j2.formatMsgNoLookups=true \
    -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:+AllowArchivingWithJavaAgent \
    -XX:+UseZGC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djdk.virtualThreadScheduler.parallelism=2"

# Optional AppCDS (Only use if archive exists)
if [ -f "app-cds.jsa" ]; then
    JAVA_OPTS="$JAVA_OPTS -Xshare:on -XX:SharedArchiveFile=app-cds.jsa"
fi

# Run the application
echo "Starting application with Java 25..."
java $JAVA_OPTS -jar $JAR_FILE
