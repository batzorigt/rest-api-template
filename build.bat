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

echo Step 1: Building with Maven...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Error: Maven build failed.
    exit /b 1
)

echo Step 2: Generating AppCDS archive...
rem This requires Java 25 and matches the Dockerfile optimization
java -XX:+UnlockDiagnosticVMOptions ^
     -XX:+AllowArchivingWithJavaAgent ^
     -XX:+UseZGC ^
     -javaagent:"%AGENT_FILE%" ^
     --add-modules java.instrument ^
     -Xshare:dump ^
     -XX:SharedArchiveFile=app-cds.jsa ^
     -jar "%JAR_FILE%"

if exist "app-cds.jsa" (
    echo Successfully generated app-cds.jsa
) else (
    echo Warning: Could not generate app-cds.jsa
)

echo Build complete. Use run.bat to start the application.

endlocal
