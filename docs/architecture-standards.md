# Architecture Standards

This document contains tool-agnostic architecture guidelines for every contributor and automation harness.

## C4 Model Usage

All new features must be documented using the C4 model:

1. **Context Diagram**: Show system boundaries and external interactions
2. **Container Diagram**: Show major software containers (HTTP server, database, etc.)
3. **Component Diagram**: Show key components within containers
4. **Class Diagram**: Show important classes and relationships (when needed)

## PlantUML Standards

All diagrams must follow these conventions:

```plantuml
@startuml
!theme plain
title [Diagram Type] - [Feature Name]

' Your diagram here

@enduml
```

- Use one consistent theme across all diagrams (`plain` is the current choice — keep it unless re-theming every diagram at once; do not invent theme names)
- Always include a title
- Keep diagrams focused on specific aspects
- Use rectangles for containers and PlantUML `component` nodes for components
- **PlantUML must render without errors** — validate diagrams after every edit (e.g., `plantuml -checkonly` or IDE preview)

## Documentation Requirements

New features require:

1. An ADR under `docs/adr/` when the feature makes a material architectural decision (create the directory and index with the first ADR)
2. Updated `docs/architecture.md` with C4 diagrams
3. Updated package structure documentation
4. API endpoint documentation (if applicable)
5. Matching tests shipped in the same task (see Testing Requirements) and the verification loop from `LOOP.md` run to green
6. Documentation neutrality preserved — apply `HARNESS.md` → *Neutrality layers* to every canonical-doc edit
7. Source neutrality preserved — code diffs are author-agnostic: no AI/agent attribution markers, no IDE metadata in git, generated code only via its generators (`HARNESS.md` → *Neutrality layers*)
8. Language: all documentation is written and maintained in English only; be concise — omit unnecessary words
9. OpenAPI contract: `openapi.yaml` is created/updated in the same task as any API-surface change (see `AGENTS.md` → Change workflow)
10. Documentation sync: every code change must create or update related docs (architecture, endpoints, config, standards) in the same task

## Testing Requirements

Every behavior change must ship with corresponding tests, verified before finishing:

1. Unit tests for new logic (utilities, parsing, hierarchy rules — pattern: `RoleTest`)
2. Handler-level HTTP tests for endpoint or authorization changes — pattern: `AuthorizationTest`
3. Authorization rules require an allow/deny matrix: 401 (no/invalid/expired token), 403 (insufficient role, data unchanged), success at and above the minimum role
4. Run `LOOP.md`'s loop (compile → targeted → full gate); a green `./mvnw test` is part of done

## Technology Standards

### Must Use

- Javalin for HTTP handling
- Ebean for database access
- MapStruct for DTO mapping
- JTE for templates
- Log4j2 for logging

### Must Not Use

- Spring Framework (use manual DI instead)
- XML application or dependency-injection wiring (library-required descriptors such as Maven and Log4j2 XML remain allowed)
- Raw JDBC (use Ebean ORM)

## Code Structure Standards

```
src/main/java/rest/api/
├── Server.java                 # Application entry point
├── Config.java                 # Configuration interface
├── Domain.java                 # Base entity
├── [FeatureName].java          # Feature-specific helpers
└── [feature]/                  # Feature packages
    ├── D[Entity].java          # Entity classes
    ├── [Entity].java           # DTO classes
    ├── [Feature]Handler.java   # API handlers
    ├── [Feature]Service.java   # Business logic
    └── query/                  # Generated QueryBean
```

## Security Standards

All endpoints must follow:

1. Role-based access control: protected mutating endpoints declare the minimum role via Javalin route args (`rest.api.Role`: `USER < MANAGER < ADMIN`, higher satisfies lower); explicitly documented entry points such as public registration may remain public; enforcement is centralized in the `Authorization` handler wrapper
2. XSRF protection (if enabled in config)
3. Input validation on all POST/PUT/DELETE
4. HTTPS termination and forwarding are deployment responsibilities; production startup rejects the shipped default encryption key
5. Security headers (automatically added)

## Monitoring Standards

Supported monitoring capabilities:

1. Metrics via Micrometer at the Basic-authenticated `/metrics` endpoint
2. Pattern-based Log4j2 request and application logs

## Database Standards

All entities must:

1. Extend `Domain` base class
2. Have `createdAt` and `updatedAt` timestamps
3. Use appropriate validation annotations
4. Follow naming conventions (lowercase, underscore)
