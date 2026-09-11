# rest-api-template

Layered REST API template: Javalin + Ebean + PostgreSQL, Java 25.
Full design: `docs/architecture.md` · agent contracts: `AGENTS.md`

## Quickstart

```bash
./build.sh        # Linux/macOS — package + AppCDS
build.bat         # Windows
# or: ./mvnw package

# Run (requires prior build)
./run.sh          # Linux/macOS
run.bat           # Windows
```

## Podman

Ebean Testcontainers invokes a Docker-compatible CLI. If Podman is installed without
the `docker` compatibility command, provide that compatibility command before tests.

## Docker

```bash
docker build -t rest-api-template .
docker run -p 8080:8080 -e DB_HOST_NAME=host.docker.internal -e DB_PASSWORD=password rest-api-template
```
Podman: add `--format docker`; use `host.containers.internal` for `DB_HOST_NAME`.

## Libraries

See `docs/architecture.md` → Technology Stack.

Transaction test coverage and targeted commands: [Testing Strategy](docs/architecture.md#testing-strategy).

## Architecture

```
HTTP request → access control → handlers → services → Ebean ORM → PostgreSQL
```
Route-level RBAC via `Authorization` wrapper; roles: `USER < MANAGER < ADMIN`. Details: `docs/architecture.md` → Security Architecture.
