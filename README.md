# Build

```
git clone git@github.com:batzorigt/rest-api-template.git
cd rest-api-template

# pull docker image into local docker registry
docker pull bellsoft/liberica-openjre-alpine-musl:20.0.2-10

# build uber jar and docker image
mvn package
```

# Run

```
cd rest-api-template/target

# run jar file
java -javaagent:../src/main/jib/ebean-agent-13.20.1.jar -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar rest-api-template-1.0.0.jar

# run as docker container
docker load -i jib-image.tar
docker run -p 8080:8080 batzorigt.rentsen.rest-api-template
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
validation: javalin built validators + custom bean validator using hibernate-validator
monitoring: micrometer plugin for prometheus
logging: log4j2
```

# Architecture

```
http requests --> filters --> handlers --> services --> orms --> rdb

Filters: Security Filters: AuthHandler, XSRFHandler, before handlers and after handlers, security header adders. 
Handlers: Receive http requests and validate request parameters or body. Call services. Manage database transactions.
Services: Business logics. Call domains to access database
ORMs: Data access layer. Easily testable and static typed & fine tuned queries which are generated automaticallly.
Helpers: Mailers, validators, template based text generator, encryptor/decryptor, base 64 encoder/decoder,
security token generetor/signer/verifier, i18n localized message loader, file loaders, configurator etc.
Exception handlers: Simple and centralized exception handling.
```
