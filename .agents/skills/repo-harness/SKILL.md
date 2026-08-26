---
name: repo-harness
description: Environment contract for this repo (JDK 25, Docker on port 6433, Maven wrapper, artifact map, failure triage, safety rails). Load before setting up tooling, running builds/tests, or diagnosing failures.
type: Playbook
title: Repo harness quick contract
resource: /HARNESS.md
tags: [harness, environment]
status: stable
stale_after: 2027-02-26T00:00:00Z
---
Distilled from `HARNESS.md` (canonical — read it fully before changing tooling/config or when triaging).

## When to load

Before environment setup, build/test runs, artifact questions, or failure diagnosis.

## Preconditions

- JDK 25 active — `pom.xml` pins source/target 25; older JDKs fail the build.
- Docker daemon running — DB-backed tests boot a PostgreSQL Testcontainer on fixed host port **6433**; free the port if busy.

## Facts

- Build/test through the Maven wrapper (`mvnw`); add `-q` for quiet compile checks. `build.*` packages plus AppCDS dump; `run.*` launches with the Ebean javaagent — bare `java -jar` fails.
- Never hand-edit generated outputs (`target/`, `jte-classes/`, `src/main/jib/`, `[feature]/query/Q*.java`, MapStruct impls, `app-cds.jsa`); fix the generating source and rebuild.
- IDE-based compilers may share `target/` with Maven: run `mvn clean` before trusting results after any IDE build.

## Post-conditions

Every environment/tooling choice matches `HARNESS.md` — its artifact/state map, runtime-config resolution order, known-failure table, and safety rails are the full contract.
