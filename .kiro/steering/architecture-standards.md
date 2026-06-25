# Architecture Standards

This steering file contains architecture guidelines for the REST API Template project.

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
2. Updated ARCHITECTURE.md with C4 diagrams
3. Updated package structure documentation
4. API endpoint documentation (if applicable)

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

1. XSRF protection (if enabled in config)
2. Input validation on all POST/PUT/DELETE
3. HTTPS enforcement in production
4. Security headers (automatically added)

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
