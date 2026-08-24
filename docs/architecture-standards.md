# Architecture Standards

This document contains the architecture guidelines for the REST API Template project. It is tool-agnostic: humans and AI coding agents alike are expected to follow it.

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
!theme cloud-blue
title [Diagram Type] - [Feature Name]

' Your diagram here

@enduml
```

- Use `cloud-blue` theme for professional appearance
- Always include a title
- Keep diagrams focused on specific aspects
- Use rectangles for containers, circles for components

## Documentation Requirements

New features require:

1. Architecture decision record (ADR)
2. Updated `docs/architecture.md` with C4 diagrams
3. Updated package structure documentation
4. API endpoint documentation (if applicable)
5. Matching tests shipped in the same task (see Testing Requirements) and the verification loop from `LOOP.md` run to green

## Testing Requirements

Every behavior change must ship with corresponding tests, verified before finishing:

1. Unit tests for new logic (utilities, parsing, hierarchy rules — pattern: `RoleTest`)
2. Handler-level HTTP tests for endpoint or authorization changes — pattern: `AuthorizationTest`
3. Authorization rules require an allow/deny matrix: 401 (no/invalid/expired token), 403 (insufficient role, data unchanged), success at and above the minimum role
4. Run `LOOP.md`'s loop (compile → targeted → full gate); a green `mvn test` is part of done

## Technology Standards

### Must Use

- Javalin for HTTP handling
- Ebean for database access
- MapStruct for DTO mapping
- JTE for templates
- Log4j2 for logging

### Must Not Use

- Spring Framework (use manual DI instead)
- XML configuration (use Java interface config)
- Raw JDBC (use Ebean ORM)

## Code Structure Standards

```
src/main/java/rest/api/
├── API.java                    # Application entry point
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

1. Role-based access control: mutating endpoints declare the minimum role via Javalin route args (`rest.api.Role`: `USER < MANAGER < ADMIN`, higher satisfies lower); enforcement is centralized in the `Authorization` handler wrapper — never hand-roll auth checks inside handlers
2. XSRF protection (if enabled in config)
3. Input validation on all POST/PUT/DELETE
4. HTTPS enforcement in production
5. Security headers (automatically added)

## Monitoring Standards

All services must expose:

1. Health check endpoint
2. Metrics via Micrometer
3. Structured logging with MDC

## Database Standards

All entities must:

1. Extend `Domain` base class
2. Have `createdAt` and `updatedAt` timestamps
3. Use appropriate validation annotations
4. Follow naming conventions (lowercase, underscore)
