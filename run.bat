@echo off
setlocal

rem Project configuration
set JAR_FILE=target\rest-api-template-1.0.0.jar
set "EBEAN_VERSION="
for /f "usebackq delims=" %%V in (`powershell -NoProfile -Command "[xml]$p=Get-Content 'pom.xml'; $p.project.properties.'ebean.version'"`) do set "EBEAN_VERSION=%%V"

if not defined EBEAN_VERSION (
    echo Error: Could not read ebean.version from pom.xml.
    exit /b 1
)

set "AGENT_FILE=src\main\jib\ebean-agent-%EBEAN_VERSION%.jar"
if not exist "%AGENT_FILE%" (
    echo Error: %AGENT_FILE% not found. Please sync the agent jar with pom.xml ebean.version.
    exit /b 1
)

rem Check if JAR exists
if not exist "%JAR_FILE%" (
    echo Error: %JAR_FILE% not found. Please run 'mvn clean package -DskipTests' first.
    exit /b 1
)

rem JVM Options (Matched with Dockerfile)
set JAVA_OPTS= ^
    -javaagent:%AGENT_FILE% ^
    -Dlog4j2.formatMsgNoLookups=true ^
    -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
    -XX:+UnlockDiagnosticVMOptions ^
    -XX:+AllowArchivingWithJavaAgent ^
    -XX:+UseZGC ^
    -XX:MaxRAMPercentage=75.0 ^
    -XX:+ExitOnOutOfMemoryError ^
    -Djdk.virtualThreadScheduler.parallelism=2

rem Optional AppCDS (Only use if archive exists)
if exist "app-cds.jsa" (
    set JAVA_OPTS=%JAVA_OPTS% -Xshare:on -XX:SharedArchiveFile=app-cds.jsa
)

rem Run the application
echo Starting application with Java 25...
java %JAVA_OPTS% -jar %JAR_FILE%

endlocal
