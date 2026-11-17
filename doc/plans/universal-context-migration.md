# UniversalContext Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or, if selected for execution,
> superpowers:subagent-driven-development to implement this plan task-by-task. Track progress with the checkboxes.

**Goal:** Make `UniversalContext` the context API used by all application consumers while preserving existing behavior.

**Architecture:** Extend the existing facade and keep `RequestContext` / `RequestContextImpl` as its internal HTTP
backend. Preserve request-scoped storage and the existing background execution path. Pass explicit node data and an
`AccessChecker` to representation builders that currently require a public copy of the request context.

**Tech Stack:** Java 21 source level, JDK 25 test environment, Spring Boot 4 / MVC, Maven, JUnit 5, Mockito.

**Spec:** The target and scope below capture the preceding migration discussion; this file is the implementation plan.
Current boundaries are documented in [runtime](../architecture/runtime.md),
[access control](../architecture/api-and-access-control.md), and
[asynchronous processing](../architecture/asynchronous-processing.md#commit-boundaries).

**Status:** Planned; no implementation has been performed.

## Target and scope

The chosen first stage replaces every application dependency on `RequestContext` with `UniversalContext` or narrower
explicit inputs. At completion, production references to `RequestContext` are confined to its declaration,
`RequestContextImpl`, and `UniversalContext`. The facade must not expose the backend in a return type or parameter.
Tests may retain backend references to construct real authorization state and inspect deferred actions.

Two approaches were considered:

- Complete the facade first: a broad but mostly mechanical migration with existing HTTP and background lifecycles.
- Remove both request-context types immediately: also requires redesigning state ownership and worker cleanup.

This plan implements the first approach. Complete removal is a separate continuation described at the end. Database
schemas, the public Node API, client applications, and background authorization semantics remain outside this stage.

## Global constraints

- Follow [AGENTS.md](../../AGENTS.md), including field injection, injected field names, and vertical argument layout.
- Name injected context fields `universalContext`; combine existing duplicate context dependencies.
- Keep accessors as value access/delegation; place new decisions in explicitly named operations or helpers.
- Preserve distinct client/admin scopes, ownership, root administration, and verified-signature authentication.
- Keep HTTP state request-scoped. Never move request identity or mutable queues into singleton fields.
- Preserve `isBackground()` and existing background methods. Do not add calls to `associate()` in migrated consumers.
- Preserve HTTP completion and background transaction behavior; do not introduce another liberin holding mechanism.
- Keep documentation under `doc/`, within 120 columns, and read `document-architecture` before editing it.
- Update the relevant architecture chapter in the task that changes its boundary; do not describe planned work as live.
- Run every Mockito test with the JDK 25 Byte Buddy agent specified below, including the first attempt.

## Review focus

| Condition | Required result | Owning task |
| --- | --- | --- |
| Limited token/carte rights or signed content | Client and admin scopes retain their separate meanings | 1, 3, 4 |
| An administrator requests public HTML or RSS | Public fields/content stay public; current authority is unchanged | 2 |
| Unknown host, provider API, or successive requests | Preserve host selection; isolate identity and queues | 1, 3 |
| Completion exception or `@NoClientId` handler | Preserve skipped actions and event client-ID handling | 3 |
| Background transaction commit, rollback, or nesting | Preserve delivery timing and held-liberin behavior | 1 |

## Change map and verification conventions

Production paths below are relative to `src/main/java/org/moera/node/`; test paths are relative to
`src/test/java/org/moera/node/`. New files are explicitly marked. Inventory remaining consumers with:

```bash
rg -l '\bRequestContext(Impl)?\b' src/main/java
rg -l '\bRequestContext(Impl)?\b|"requestContext"' src/test/java
```

The inspected tree contains 102 production files mentioning `RequestContext` and 22 test files mentioning either
request-context type. Use the searches as the authoritative inventory when executing, rather than freezing a file list.
Whenever a class is migrated, update its test injection and mocks in the same task so intermediate tasks compile.

Set the agent path in the shell used for all test commands:

```bash
MOERA_BYTE_BUDDY_AGENT=/home/balu/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar
```

Each task must leave compilation and its focused tests passing. For new behavior contracts, add the named tests first
and confirm the expected failure before implementation. Existing behavior characterizations may pass immediately.
Mechanical dependency replacements need adapted existing tests, not a new test for every delegating method.

## Task 1: Complete the facade without changing execution boundaries

**Files:** Modify `global/UniversalContext.java`, `global/RequestContext.java`, and test
`global/UniversalContextTest.java`.
Create tests `global/UniversalContextRequestScopeTest.java` and `global/UniversalContextLiberinTest.java`.
Update `doc/architecture/runtime.md` in this task to identify the facade as the consumer API.

**Interfaces:** Produce `UniversalContext implements AccessChecker` and
`boolean isClient(String name, Scope scope)`. Add every currently missing `RequestContext` method signature except
`getPublic()`, using the [interface](../../src/main/java/org/moera/node/global/RequestContext.java) as the exact source.
Existing `UniversalContext` methods keep their current contracts.

The new methods have these explicit contracts:

| Methods | HTTP | Background |
| --- | --- | --- |
| `isClient(String, Scope)` | Compare scoped identity with `Objects.equals` | Use existing scoped identity access |
| New scope readers and identity setters | Delegate to the HTTP backend | Require an active HTTP request |
| Root/sheriff flags, domain setup, HTTP metadata, timings | Delegate | Require an active HTTP request |
| New deferred-action writers and readers | Use existing request queues/flags | Require an active HTTP request |

The last group includes `invalidateFriendCache`, `blockedUsersUpdated`, and `visitPosting`.
This stage adds no background callers for them. Keep existing dual-mode `subscriptionsUpdated()` and
`invalidateSubscribedCache()` intact.
Request-only methods may use the existing scoped proxy's missing-request failure; do not fabricate default values.

- [ ] Extend `UniversalContextTest` with `accessCheckerPreservesSeparateScopes` and `isClientRespectsIdentityScope`.
  Use the real request backend, the existing `bob_0` identity and friend group, and distinct
  `DELETE_OWN_CONTENT` / `DELETE_OTHERS_CONTENT` scopes. Assert that passing the facade as `AccessChecker` preserves
  the current principal decisions; verify the new identity helper in HTTP and signed background execution.
- [ ] Add `UniversalContextRequestScopeTest` using a minimal Spring web context with the real scoped proxy and mocked
  dependencies. Test two sequential requests with different options, identities, and pending liberins; the second
  starts with its own state. Test that HTTP reads override pre-existing background state and request-only access after
  unbinding the request fails with Spring's inactive-scope error. Always reset request attributes in teardown.
- [ ] Add `UniversalContextLiberinTest` with a real `UniversalContext` and the existing `util/Transaction` helper.
  Supply a mocked transaction manager and liberin manager. Assert that outside-transaction background sends are
  immediate, successful commit sends once, rollback/commit failure sends nothing, and nested holds merge into the
  enclosing hold. Do not change the existing `REQUIRES_NEW` database behavior or nested hold semantics.
- [ ] Run the focused tests; the `AccessChecker` assignment/new helper tests must expose the missing API before the
  facade is extended. Existing transaction characterizations establish the behavior to preserve.
- [ ] Implement the facade additions by delegation and mark the existing two-argument `isPrincipal` as `@Override`.
  Add `UniversalContext.Times` with `RECEIVED = 0`, `STARTED = 1`, and `FINISHED = 2`. Retain the old constants until
  the interceptor imports move in Task 3. Keep the facade's existing dependency set.
- [ ] Revise the request/background section of the runtime chapter to describe the consumer boundary and the continuing
  HTTP-only operations, linking to the context code rather than listing its methods.
- [ ] Verify with the command below; expect compilation success and zero test failures.

```bash
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" -Dtest='UniversalContext*Test' test
```

## Task 2: Remove public context copies from representation building

**Files:** Modify `model/NodeNameInfoUtil.java`, `model/ProfileInfoUtil.java`, `model/WhoAmIiUtil.java`,
`rest/NodeNameController.java`, `rest/ProfileController.java`, `rest/WhoAmIiController.java`,
`ui/GlobalsControllerAdvice.java`, `ui/IndexUiController.java`, `ui/TitleBuilder.java`, and `ui/RssController.java`.
Remove `getPublic()` from `global/RequestContext.java` and `global/RequestContextImpl.java` after migrating its callers.
Modify test `model/ProfileSourceUriTest.java`; create tests `model/PublicProfileRenderingTest.java` and
`ui/PublicUiContextTest.java`. Update the representation rule in `doc/architecture/api-and-access-control.md`.

**Interfaces:** Replace the context-dependent builder signatures with:

```java
NodeNameInfo NodeNameInfoUtil.build(Options options, AccessChecker accessChecker)
ProfileInfo ProfileInfoUtil.build(
    Options options,
    Avatar avatar,
    AccessChecker accessChecker,
    boolean includeSource,
    DirectServeOperations directServe
)
WhoAmI WhoAmIiUtil.build(Options options, Avatar avatar, DirectServeOperations directServe)
```

These are static methods. Keep the existing unrestricted `ProfileInfoUtil.build(Options)` overload unchanged; public
rendering must use the overload with an explicit checker. Use `isPrincipal(Principal.ADMIN, scope)` for the existing
administrative field gates. `NodeNameInfoUtil` must still tolerate null options for an undefined domain.

- [ ] Add `PublicProfileRenderingTest` cases for an admin-only email and naming-operation metadata: `PUBLIC` hides them,
  an appropriately scoped checker exposes them, and an unrelated admin scope does not. Preserve public email visibility,
  source inclusion, avatar output, and the null-options naming result. Adapt `ProfileSourceUriTest` to explicit inputs.
- [ ] Add `PublicUiContextTest` using a real admin-authorized facade: public profile and global naming models omit
  protected fields, RSS excludes non-public stories/entries, and building these representations leaves the original
  admin/client scopes unchanged. Verify that titles, site links, and avatar links remain populated.
- [ ] Run the new tests to expose the missing signatures or public-view regressions.
- [ ] Implement the signatures and migrate all builder call sites. REST passes `universalContext` as checker; public
  HTML passes `AccessCheckers.PUBLIC`. Read node identity/options/avatar/site URL from the facade. RSS retains its
  existing public story and entry filters; title/RSS helpers no longer construct a public context to read node data.
- [ ] Remove `getPublic()` after `rg -n '\.getPublic\(' src/main/java/org/moera/node` confirms no remaining callers.
  Update the access-control chapter's representation rule with the explicit public-checker boundary.
- [ ] Verify the following focused tests, expecting zero failures:

```bash
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dtest='ProfileSourceUriTest,PublicProfileRenderingTest,PublicUiContextTest' test
```

## Task 3: Migrate HTTP initialization and completion

**Files:** Modify `domain/DomainInterceptor.java`, all remaining request-context consumers in `auth/` and `global/`,
and `liberin/AfterCommitLiberinsInterceptor.java`; retain the backend dependency inside `UniversalContext`.
Remove the old `RequestContext.Times` constants after changing both slow-request interceptors' imports.
Modify tests `global/RateLimitInterceptorTest.java`, `rest/PostingControllerDeleteAuthorizationTest.java`, and
`auth/TokenHashingIT.java` as their injected classes change. Create tests `domain/DomainInterceptorTest.java` and
`liberin/AfterCommitLiberinsInterceptorTest.java`. Review `event/EventManager.java` and its existing test for calls to
`AuthenticationManager`. Revise `doc/architecture/asynchronous-processing.md` to describe the facade entry point.

**Interfaces:** Use the API from Task 1. Preserve interceptor callback signatures, registration order in
`MoeraNodeApplication`, and current `AuthenticationManager` entry points.

- [ ] Add domain-interceptor cases for a known host, an unknown host accepted by `@ProviderApi`, an ordinary rejected
  unknown host, and single-host fallback. Assert that the provider case leaves node/options undefined and that virtual
  page and root-admin handling retain their existing behavior.
- [ ] Add completion-interceptor cases with a real facade/backend and mocked recipients. Successful completion must
  preserve cache invalidation, visit recording, liberin dispatch, subscription rescan, and blocked-user recalculation
  order. Assert assigned `nodeId` and `clientId`; repeat with `@NoClientId` expecting a null client ID. A non-null
  completion exception must trigger none of these actions. Preserve the existing `ex == null` criterion exactly.
- [ ] Characterize the tests before changing wiring, then migrate the interceptors and `AuthenticationManager` to one
  `universalContext` field. Keep request-specific IP/User-Agent reads on their existing guarded paths; preserve the
  current WebSocket authentication behavior without adding HTTP assumptions or unrelated authentication fixes.
- [ ] Adapt affected test fixtures immediately. Where the authorization tests construct `RequestContextImpl`, wrap that
  same instance in a real `UniversalContext` and bind request attributes; do not replace scope decisions with a mock
  that grants every permission. Reset bindings in teardown.
- [ ] Update the asynchronous-processing chapter without changing its guarantees: HTTP actions still come from request
  completion, background liberins still follow `Transaction`, and neither path becomes a durable outbox.
- [ ] Run the tests below. Expect preserved authorization, rate-limit, initialization, and completion results.

```bash
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dtest='DomainInterceptorTest,AfterCommitLiberinsInterceptorTest,RateLimitInterceptorTest,EventManagerTest' test
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" -Dtest=PostingControllerDeleteAuthorizationTest test
```

## Task 4: Migrate the remaining services, controllers, and Web UI helpers

**Files:** Remaining matches from the production inventory in `operations/`, `rest/`, `rest/notification/`, `ui/`,
`ui/helper/`, and `media/grant/`; remove the unused request-context injection from `task/TaskAutowire.java`.
Combine duplicate context fields in `operations/FeedOperations.java` and `operations/BlockedUserOperations.java`.
Adapt matching tests under `operations/`, `rest/`, `ui/helper/`, and `auth/`.

**Interfaces:** Consume the completed facade and builder signatures from Tasks 1 and 2. Methods whose actual dependency
is `AccessChecker` continue accepting that interface; they do not acquire a singleton-context dependency.

- [ ] Replace injected types, field names, imports, method references, local context variables, and any remaining
  context-typed parameters. Preserve method argument layout and all authorization/transaction expressions.
- [ ] Update corresponding mock types, `ReflectionTestUtils` field names, and shared fixtures in the same changes.
  If a test previously provided both contexts, make them use one facade with consistent options and identity.
  Authorization tests that exercise the real permission implementation must keep doing so through the facade.
- [ ] Run focused existing tests for each migrated group. In particular, run both deletion authorization suites and
  tests for posting info, story/field visibility, tokens, reactions, and UI helpers. Reuse coverage for field renames.
- [ ] Run the inventory again. The production result must contain exactly `global/RequestContext.java`,
  `global/RequestContextImpl.java`, and `global/UniversalContext.java`; all other matches are unfinished migration work.

```bash
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dtest='CommentAndReactionDeleteAuthorizationTest,PostingControllerDeleteAuthorizationTest' test
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dtest='PostingInfoOperationsTest,StoryController*Test,TokenControllerTest,ReactionMutationEventsTest' test
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" -Dtest=MoeraHelperSourceTest test
rg -l '\bRequestContext(Impl)?\b' src/main/java
```

## Task 5: Verify the migration as a whole

**Files:** Review the files changed by Tasks 1-4 and the three affected architecture chapters. No new architecture
chapter or index entry is needed. Check the repository's existing `doc/architecture.md` links remain correct.

**Interfaces:** The final consumer boundary is `UniversalContext` or explicit data plus `AccessChecker`; the HTTP
backend stays internal. The request/background boundary and completion contracts remain those stated above.

- [ ] Run `git diff --check` and review the production inventory. Confirm no `getPublic()` replacement exposes the
  request backend and no caller uses `UniversalContext.associate()` as part of this migration.
- [ ] Run the full default Maven test lifecycle once after the focused checks pass. This also runs the configured
  Checkstyle validation and compiles all test sources; expect `BUILD SUCCESS` and zero failures.
- [ ] Explicitly run changed database integration tests, including `TokenHashingIT` and `DraftControllerIT`, separately
  against disposable PostgreSQL databases via `-Dmoera.test.database`. They are opt-in tests and are not established as
  passing by the default test run. Report unavailable database runs as unverified rather than passed.
- [ ] Review dependency wiring with a Spring application startup in the normal local development configuration.
  Confirm that replacing direct request-context injection has not introduced eager dependency cycles. Exercise a
  public page, a scoped REST request, and one normal background task with their expected node identities.
- [ ] Review the architecture changes for the implemented state, valid links, one primary home per topic, and the
  120-character line limit. Summarize test evidence and any environmental limitations before integration.

```bash
git diff --check
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" test
# Set MOERA_TEST_DATABASE to a dedicated disposable database for each integration-test invocation.
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dmoera.test.database="$MOERA_TEST_DATABASE" -Dtest=TokenHashingIT test
mvn "-DargLine=-javaagent:$MOERA_BYTE_BUDDY_AGENT" \
  -Dmoera.test.database="$MOERA_TEST_DATABASE" -Dtest=DraftControllerIT test
```

## Separate continuation: removing the HTTP backend types

Complete deletion of `RequestContext` and `RequestContextImpl` requires a subsequent state-lifecycle design. The facade
migration above makes that change local instead of repeating the consumer migration. That continuation must cover:

- A data holder bound to the HTTP request, with common logic owned by `UniversalContext`; preserve request lifetime
  across dispatches and the current behavior of requests without a selected domain.
- A defined background state lifetime. Today `associate()` updates only node identity/MDC, and `Task.run()` does not
  clear the thread-local authority afterward. Define fresh-state and restore/cleanup operations at actual worker
  boundaries, including `Task`, liberin processing, scheduled tenant scans, and WebSocket handling where applicable.
- Cleanup in `finally` and restoration for nested execution, without discarding an enclosing transaction's held
  liberins. Cover an authenticated task followed by an unauthenticated task on the same worker and cross-node reuse.
- An explicit decision on which currently request-only operations gain background semantics, including client scopes,
  HTTP metadata, visit tracking, and deferred cache actions. Moving storage alone does not define these contracts.

Do not include those lifecycle changes as incidental cleanup while executing this plan.
