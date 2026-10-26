# AI Agent Development Guide

## Project Overview

**Foundation MyBatis Util** — A plain Java library of reusable MyBatis type handlers (`com.iqkv.foundation.mybatis.util`). No Spring Boot application, no web layer, no DI container. It exists so that platform services stop copying the same JDBC type handlers into every repository.

**Key characteristics:**

- Java 25, single Maven module, parent `com.iqkv:boot-parent-pom`
- Three type handlers: `UuidTypeHandler`, `JsonTypeHandler`, `StringArrayTypeHandler`
- MyBatis (`mybatis-spring-boot-starter`) and the PostgreSQL driver are `optional` dependencies — the consumers already provide them
- JUnit Jupiter 6 + AssertJ + Mockito for testing; tests co-located with source in `src/test/`
- Checkstyle enforced at `validate` phase via `maven-project-common-checkstyle.xml` from `com.iqkv:checkstyle-config`
- JaCoCo coverage gate: ≥ 90% instruction coverage, no uncovered class
- Node.js tooling (pnpm + Husky) for commit hooks and formatting only — not part of the Java build

## Project Structure

```
foundation-mybatis-util/
├── src/
│   ├── main/java/com/iqkv/foundation/mybatis/util/
│   │   ├── UuidTypeHandler.java        # UUID <-> uuid / varchar
│   │   ├── JsonTypeHandler.java        # raw JSON String <-> jsonb
│   │   └── StringArrayTypeHandler.java # List<String> <-> text[]
│   └── test/java/com/iqkv/foundation/mybatis/util/
│       ├── UuidTypeHandlerTest.java
│       ├── JsonTypeHandlerTest.java
│       └── StringArrayTypeHandlerTest.java
├── pom.xml                        # Single-module Maven build; Java 25
├── package.json                   # Node tooling only (Husky, OxFmt, commitlint)
└── AGENTS.md
```

## Code Standards

### Type handler pattern

Every handler follows the MyBatis `BaseTypeHandler` contract:

- `public class XxxTypeHandler extends BaseTypeHandler<T>` with a class Javadoc naming the Java type and the SQL type it maps
- `@MappedTypes(...)` when the Java type identifies the handler unambiguously (`UUID`, `List`); omit it when the Java type is ambiguous (`String` → `JsonTypeHandler` must be referenced explicitly in XML)
- All four members: `setNonNullParameter` plus the three `getNullableResult` overloads (`ResultSet` by name, `ResultSet` by index, `CallableStatement` by index)
- Null results return `null` (or `Collections.emptyList()` for array handlers) — never throw
- Parameters and local variables are `final`; follow the Checkstyle import order (static imports first, then `java.*`, then third-party)
- A dependency used only by one handler (the PostgreSQL driver in `JsonTypeHandler`) is documented in the class Javadoc

```java
/**
 * MyBatis type handler for {@link UUID} &harr; SQL {@code uuid} / {@code varchar} columns.
 */
@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {
  // ...
}
```

### Testing standards

- Test class name: `{ClassName}Test`, same package as the class under test
- Use `@DisplayName` on every `@Test` — describe the expected behavior, not the method name
- Follow **Arrange / Act / Assert** — no logic in assertions
- Use AssertJ fluent assertions (`assertThat(...).isEqualTo(...)`) — not JUnit `assertEquals`
- Mock `java.sql` collaborators with plain `Mockito.mock(...)` in `@BeforeEach`; `mockito-junit-jupiter` is intentionally not a dependency
- Cover every `getNullableResult` overload, both the value and the `null` branch
- When Mockito matchers are used, all arguments must be matchers (`eq(1)`, `any()`) — mixing raw values fails at runtime
- Assert the actual SQL binding, not just the call: capture the argument and check the `PGobject` type, the `text[]` content, etc.

```java
@Test
@DisplayName("getNullableResult by column name returns null when the column is null")
void shouldReturnNullByColumnName() throws SQLException {
  // Act
  final var result = handler.getNullableResult(resultSet, "id");

  // Assert
  assertThat(result).isNull();
}
```

### Checkstyle

The build runs Checkstyle at `validate` phase. Config is pulled from `com.iqkv:checkstyle-config` (`maven-project-common-checkstyle.xml`). Do not suppress violations with `@SuppressWarnings("checkstyle:...")` without a clear justification comment.

## Execution Discipline

- Root cause first. Fix the real entry point, not a bypass around it.
- Read the existing handler and its test before adding to or modifying either.
- After two identical build failures without new evidence, change approach — do not retry blindly.
- Run `./mvnw verify` locally before presenting a result; report failures rather than assuming green.
- No speculative additions. Add a handler only when a real consumer duplicates one.

## Minimal Change Discipline

Solve the whole problem with the least new code. The best code is the code never written.

### Scope before you write

Read the task and the code it touches. List every place the change must reach: callers, tests, consumers, config. Check what it could break for existing services. That is scope. Extra features are not.

### Pick the smallest complete change

Take the first option that fully works:

1. Does it need to exist? Drop handlers, options and flexibility nobody asked for — name them in one line so the user can decide.
2. Already in this library (handler, test helper, pattern)? Use it the way the surrounding code does.
3. Standard library or platform feature? Use it, unless the project has its own.
4. An installed dependency? Use it. Never add a dependency for a few lines.
5. Can it be one line a reader gets at a glance? One line.
6. Otherwise: the minimum code that works.

Be lazy about the solution, never about the change itself:

- Finish every part the task needs, including the tests the change breaks.
- No abstraction, wrapper, option, config or "for later" code nobody asked for. Deletion beats addition.
- The shortest working diff wins, once you know everything it must touch. A one-liner that needs decoding is not short.
- Comment only the why the code cannot show, in one line.
- Code you move or merge keeps its error handling, Javadoc and null semantics.
- Between options of equal size, take the one that is correct on edge cases.
- Every new branch needs a test that exercises it — the coverage gate is not advisory.

Never cut: null handling, the full `getNullableResult` overload set, anything the user explicitly asked for.

### Token discipline

- Locate with Grep/Glob, then read only the ranges you need — not whole trees or whole files.
- Prefer editing existing files to creating new ones; reuse code that is already there before writing.
- Never paste file contents back in a reply. Cite the path and lines.
- Keep replies to the outcome: what changed, what passed, any risk.

## Security

- No secrets, tokens, or credentials belong in this library or its tests.
- Use synthetic data in tests — no real email addresses, names, or identifiers.
- Flag unusual dependency names before adding them to `pom.xml`. Use exact versions.

## AI Agent Development Guidelines

### Code generation principles

1. **Reuse a handler before creating one** — only create a new class for a genuinely distinct JDBC mapping
2. **Test alongside**: every new handler or branch needs a corresponding test in the matching `*Test.java` file
3. **Null-safe always**: every `getNullableResult` documents and handles null
4. **No new dependencies** without explicit approval — keep the footprint at MyBatis + PostgreSQL driver, both `optional`
5. **Checkstyle-clean**: run `./mvnw checkstyle:check` to validate before presenting changes

### Approval workflow (MANDATORY)

Ask before applying. For any file creation, modification, or deletion:

```
1. ANALYZE  → understand the request
2. PRESENT  → describe changes, show handler signature + Javadoc, list affected files
3. WAIT     → stop and wait for explicit approval
4. APPLY    → only after approval
5. VERIFY   → run ./mvnw verify, report results concisely
```

**Approval phrases:** "Yes", "Proceed", "Apply", "Do it", "Go ahead", "Looks good"

Operations NOT requiring approval: reading files, explaining concepts, running type checks.

### When presenting changes

````markdown
## Proposed Changes

**Goal**: one sentence

**Files**:

- `src/main/.../UuidTypeHandler.java` — map `UUID` to `uuid`
- `src/test/.../UuidTypeHandlerTest.java` — add corresponding tests

**New type signature**:

```java
@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> { ... }
```
````

Proceed?

## Commit Standards

Format: `type(scope): subject`

- Subject: imperative, lowercase, no trailing period, ≤ 72 chars
- Types: `feat`, `fix`, `improvement`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`, `build`, `revert`
- Scope: affected handler or area (e.g., `uuid-handler`, `json-handler`, `string-array-handler`, `checkstyle`, `deps`)
- For `fix`: describe the symptom and trigger, not the code change
    - ✅ `fix(uuid-handler): null uuid column throws NPE on read`
    - ❌ `fix(uuid-handler): add null check in getNullableResult`

Examples:

- `feat(json-handler): add jsonb type handler`
- `test(string-array-handler): cover the null array content branch`
- `chore(deps): bump postgresql driver to 42.7.14`
- `fix(uuid-handler): null uuid column throws NPE on read`

## Development Commands

```bash
# Build and test (runs Checkstyle + JUnit + package)
./mvnw verify

# Tests only
./mvnw test

# Checkstyle only
./mvnw checkstyle:check

# Skip Checkstyle for a quick local build
./mvnw verify -Dcheckstyle.skip=true

# Node tooling (formatting only — not part of Java build)
pnpm formatter:check   # check formatting
pnpm formatter:write   # auto-format
```

## Evaluation

How we judge whether an agent task in this repository is actually done. Evaluation is evidence-based — a claim without the command and its observed result is not a pass.

### Definition of done

A task is evaluated as done only when all of the following hold:

- Every gate required by the change type below has run and passed.
- The final report names the exact command(s) run and the observed result (pass/fail).
- No required gate was skipped silently. A gate that could not run is reported as a gap, never as a pass.

### Required gates by change type

| Change type                           | Required gates                                                                  |
| ------------------------------------- | ------------------------------------------------------------------------------- |
| Any change                            | `./mvnw -q compile`, then the narrowest targeted `./mvnw test -Dtest=ClassName` |
| New or changed type handler           | Unit tests for every handler member, then full `./mvnw verify`                  |
| Null / branch handling logic          | Unit tests covering both branches, then full `./mvnw verify`                    |
| Dependency or build config            | Full `./mvnw verify`                                                            |
| Formatting or docs (`AGENTS.md`) only | `pnpm formatter:check`; no Java code gate — state that none applies             |

### Ordering

Run the narrowest relevant gate first, then the full set for the change type. On the first failure, stop and report it — do not continue on an unproven base.

## Verification After Changes

Run before presenting a result:

```bash
./mvnw verify
```

This covers: Checkstyle → compile → test → package. Report results in 2–3 sentences. Generate a commit message for any multi-file change.
