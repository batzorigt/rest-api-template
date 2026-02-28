# Build

```
git clone git@github.com:batzorigt/rest-api-template.git
cd /path/to/rest-api-template

# create a file symbolic link to podman on linux like os if you have installed podman instead of docker
ln -s $(which podman) /usr/local/bin/docker

# create a file symbolic link to podman on windows powershell if you have installed podman instead of docker
$podmanDir = "C:\Users\your-name\AppData\Local\Programs\Podman"
New-Item -ItemType SymbolicLink `
  -Path   "$podmanDir\docker.exe" `
  -Target "$podmanDir\podman.exe"

# build. for uber jar and docker image, need to enable (uncomment correspondent section) maven-shade-plugin and jib-maven-plugin
mvn package
```

# Run

```
cd /path/to/rest-api-template/target

# run jar file
java "-javaagent:../src/main/jib/ebean-agent-17.3.0.jar" "-Dlog4j2.formatMsgNoLookups=true" "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector" -jar rest-api-template-1.0.0.jar

# run as docker container if you enabled jib-maven-plugin
docker load -i jib-image.tar
docker run -p 8080:8080 -e DB_HOST_NAME=host.docker.internal batzorigt.rentsen.rest-api-template

# run as docker container using Dockerfile
cd /path/to/rest-api-template
docker build -t batzorigt.rentsen/rest-api-template:20260228 .
docker run -p 8080:8080 -e DB_HOST_NAME=host.docker.internal batzorigt.rentsen/rest-api-template:20260228
```

# IntelliJ IDEA

```
https://projectlombok.org/setup/intellij
https://ebean.io/docs/getting-started/intellij-idea
https://mapstruct.org/documentation/ide-support/
```

# Eclipse
```
https://projectlombok.org/setup/eclipse
https://ebean.io/docs/getting-started/eclipse-ide
https://mapstruct.org/documentation/ide-support/
```

# Used Libraries

```
web framework: https://javalin.io/

configuration: https://matteobaccan.github.io/owner/

orm: https://ebean.io/

bean mappings: https://mapstruct.org/

templating: https://jte.gg/

validation: javalin built-in validators + custom java bean validator using hibernate-validator

monitoring: micrometer plugin for prometheus

logging: log4j2

testing: JUnit5, Testcontainers for Java, mockito, jmockit
```

# Architecture

```
http requests --> filters --> handlers --> services --> orms --> rdb

Filters:
AuthHandler, XSRFHandler, before handlers and after handlers, security header adders. 

Handlers:
Receive http requests and validate request parameters or body. Call services. Manage database transactions.

Services:
Business logics. Call domains to access database

ORMs:
Data access layer. Easily testable and static typed & fine tuned queries which are generated automaticallly.

Helpers:
Mailers, validators, template based text generator, encryptor/decryptor, base 64 encoder/decoder,
security token generator/signer/verifier, i18n localized message loader, file loaders, configurator etc.

Exception handlers:
Simple and centralized exception handling.
```
