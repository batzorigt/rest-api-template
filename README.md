# rest-api-template

Layered REST API template: Javalin + Ebean + PostgreSQL, Java 25.
Full design: `docs/architecture.md` · agent contracts: `AGENTS.md`

## Quickstart

```bash
./build.sh        # Linux/macOS — package + AppCDS
build.bat         # Windows
# or: mvn package

# Run (requires prior build)
./run.sh          # Linux/macOS
run.bat           # Windows
```

## Podman

Symlink `podman` as `docker` (see `AGENTS.md` → Commands).

## Docker

```bash
docker build -t rest-api-template .
docker run -p 8080:8080 -e DB_HOST_NAME=host.docker.internal -e DB_PASSWORD=password rest-api-template
```
Podman: add `--format docker`; use `host.containers.internal` for `DB_HOST_NAME`.

## IDE Setup

- IntelliJ: Lombok, Ebean, MapStruct (see `AGENTS.md` → Gotchas)
- Eclipse: Lombok, Ebean (see `AGENTS.md` → Gotchas)

## Libraries

See `docs/architecture.md` → Technology Stack.

## Architecture

```
http request → access control → handlers → services → orms → rdb
```
Route-level RBAC via `Authorization` wrapper; roles: `USER < MANAGER < ADMIN`. Details: `docs/architecture.md` → Security Architecture.