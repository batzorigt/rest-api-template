# docs index

Progressive-disclosure map for this directory. Grep these stable headings inside [architecture.md](architecture.md) instead of loading it whole. Backticked labels are validated as exact Markdown headings.

## architecture.md — section anchors

- `Overview` — what the system is, one-screen context
- `C4 Level 1 — System Context` — actors and external systems
- `C4 Level 2 — Container` — deployable units and data stores
- `C4 Level 3 — Component (API Layer)` — filters, handlers, wrappers
- `C4 Level 3 — Component (Domain Layer)` — entities, services, DTOs
- `Request Flow` — end-to-end path of an HTTP request
- `Security Architecture` — headers, XSRF, RBAC/Authorization (subsections: Security Headers (every response), Authorization (RBAC))
- `Database Schema` — tables generated from Ebean entities
- `Package Structure` — full source tree with per-file annotations
- `Technology Stack` — libraries and versions in use
- `API Endpoints` — route table with roles column (subsections: Pagination Query Params, Error Responses)
- `Build & Deployment` — Maven, AppCDS, Jib, run scripts
- `Testing Strategy` — Testcontainers setup, handler-test patterns
- `Configuration Reference` — every config key with defaults

## standards

- [architecture-standards.md](architecture-standards.md) — team hard rules: C4/PlantUML requirements, must-use/must-not-use technologies, security and database standards

## ai-loop

- `.ai-loop/schema/` — JSON schemas for state and verification tracking
- `.ai-loop/templates/` — templates for task planning, state, and verification
- `.ai-loop/tasks/` — per-task state and verification tracking
- `scripts/validate-ai-loop.*` — validation scripts for AI workflow integrity
