---
taskId: task-20260909-090000
status: completed
createdAt: 2026-09-09T09:00:00Z
---

# Task: Verify handler transactions with JUnit 6

Add database-backed tests for all five transactional methods in GenreHandler and
MemberHandler. Base commit: 3720b054feadb50c6178b70f54d329ee2fc32ac6. Initial tree
was clean on feature/ai-coding-workflow; work uses a separate local branch.

1. Reuse existing JUnit 6.0.3, Mockito 5.23.0, Ebean 17.6.0 and test database setup.
2. Invoke real enhanced handlers with mocked contexts and real services/database.
   For read handlers, verify `@Transactional(readOnly = true)` metadata plus an
   active transaction that closes after success or failure. For write handlers,
   verify writable transactions, commits, rollback after writes, member phone
   cascade atomicity and cleanup. Do not wrap tests in a test transaction or
   change production endpoints.
3. Share transaction assertions in HandlerTransactionTestSupport; keep feature
   fixtures and persistence assertions in feature test classes.
4. Update the architecture testing strategy and README pointer without repeating
   environment contracts. No dependency or API surface changes are needed.
5. Run clean quiet compile, both targeted transaction test suites, full Maven test
   gate, workflow validation and diff checks. Use the clean build because an
   editor may share compiler output. Inspect command output, not generated files.

Reviewed transaction semantics in https://ebean.io/docs/transactions/ and test
support in https://docs.junit.org/current/user-guide/ and
https://javalin.io/documentation#testing. Tests use existing repository versions.

The existing Podman machine was stopped; starting it restores the required test
environment. Database fixtures use test-only values and are cleaned after tests.
No production behavior, security, availability or schema contract changes.

The initial runtime `Transaction.isReadOnly()` assertion failed because this test
database has no configured read-only datasource. The user approved this revised
annotation-plus-lifecycle approach on 2026-09-10; the existing corrective-attempt
history remains recorded in state.json.

Primary implementation count: 1. Corrective attempts: 0. Transient retries: 0.
Flaky reruns: 0.
