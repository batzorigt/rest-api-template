---
description: Entity-change loop guardrail (migration generator is IDE-only)
agent: general
---
$ARGUMENTS

`rest.api.GenerateDbMigration#main` is excluded from the Maven build (compiler excludes in `pom.xml`). Do NOT attempt `mvn exec`, `mvnw` or `java -cp` invocations of it — they cannot work; tell the user to run it from their IDE instead. Meanwhile finish the rest of LOOP.md's entity-change loop, and flag the pending migration for manual review: tests use `ddlMode=dropCreate`, so green tests never validate new `dbmigration/*.sql`.
