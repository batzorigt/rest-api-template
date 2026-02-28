#!/bin/bash

# Project configuration
JAR_FILE="target/rest-api-template-1.0.0.jar"
AGENT_FILE="src/main/jib/ebean-agent-17.3.0.jar"

echo "Step 1: Building with Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Maven build failed."
    exit 1
fi

echo "Step 2: Generating AppCDS archive..."
# This requires Java 25 and matches the Dockerfile optimization
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+AllowArchivingWithJavaAgent \
     -XX:+UseZGC \
     -javaagent:"$AGENT_FILE" \
     --add-modules java.instrument \
     -Xshare:dump \
     -XX:SharedArchiveFile=app-cds.jsa \
     -jar "$JAR_FILE" || true

if [ -f "app-cds.jsa" ]; then
    echo "Successfully generated app-cds.jsa"
else
    echo "Warning: Could not generate app-cds.jsa"
fi

echo "Build complete. Use ./run.sh to start the application."
