# Runtime and node contexts

## Hosted domains

`Domains` maps HTTP domain names and internal node UUIDs to per-node `Options`. It loads this mapping after option
metadata is configured. A fresh database receives a `_default_` domain and the initial per-node records. Once domains
are configured, a `DomainsConfiguredEvent` starts components that restore pending jobs, notifications, and picks.

`node.multi` selects `NONE`, `PRIVATE`, or `PUBLIC` hosting. In single-host mode an arbitrary request host resolves to
the default domain. In multi-host modes, `DomainInterceptor` accepts configured hosts; provider endpoints remain
available for provisioning operations on an otherwise unknown host. `node.domain` supplies the server's default DNS
domain and is also required for public registration.

Most tenant-owned rows contain `node_id`. It is the stable partition key even when a hosted domain name or the node's
registered Moera name changes.

## Request context

`RequestContext` is a request-scoped object populated by MVC interceptors. It carries:

- the selected node ID, node name, profile metadata, and per-node options;
- the effective site and request URLs;
- local admin and client scopes;
- the authenticated remote node, friend-group membership, and subscription relationship;
- user-agent, remote-address, and client-session information; and
- deferred liberins and cache invalidations that are applied after a successful request.

The main interceptor sequence performs request-rate accounting, domain selection, user-agent and crawler detection,
authentication, rate limiting, virtual-page handling, cache headers, client-ID capture, after-commit dispatch, and
entitlement checks. Slow-request timing can wrap this sequence when enabled.

## Background context

`UniversalContext` presents the same node and authorization interface to request code and background tasks. During an
HTTP request it delegates to `RequestContext`. In a background thread it uses thread-local state associated by
`TaskAutowire` or by a task that intentionally switches to a particular node.

Background work must be associated with the relevant node before reading options, profile data, signing keys, or
tenant-owned records. Global jobs may run without a node until they encounter node-specific work; for example, video
compression groups media owners by node and associates each group before creating replacement owners and events.

`UniversalContext.associate()` changes the context of the whole thread. It is therefore limited to code semantically
bound to a concrete job or background task.

## Transactions

HTTP controller methods commonly use Jakarta `@Transactional`. Background and maintenance components use the
`Transaction` helper, whose read and write methods create `REQUIRES_NEW` transactions and expose variants that either
propagate or handle exceptions.

`Transaction` also coordinates background liberins. It starts a holding scope with every transaction, sends held
liberins only after the database commit succeeds, and discards them on rollback. Nested scopes merge into their parent
until the outer commit. HTTP requests keep deferred liberins in `RequestContext`; `AfterCommitLiberinsInterceptor`
dispatches them after the controller transaction has completed successfully.

Spring JPA open-in-view is disabled. Callers must load or convert lazy relationships inside an explicit transaction.
This is especially relevant when building media previews, events, or federation payloads.

## Executors and schedulers

`MoeraNodeApplication` creates fixed-size pools from `node.pools`:

| Bean | Main use |
| --- | --- |
| `namingTaskExecutor` | naming service work |
| `remoteTaskExecutor` | persistent jobs and remote operations |
| `notificationSenderTaskExecutor` | outgoing federation notifications |
| `pickerTaskExecutor` | downloading remote postings and comments |
| `pushTaskExecutor` | local push and streaming work |
| `eventScheduler` | client event retry and heartbeat scheduling |
| `taskScheduler` | other `@Scheduled` work |

The pools bound concurrency inside one process. Database leases, row locks, advisory locks, and idempotent state
transitions provide coordination between multiple application instances.

## Configuration layers

Deployment-wide configuration is bound from Spring properties into `Config` and its nested classes. It includes
hosting mode, addresses, thread-pool sizes, root credentials, mail, media storage, external services, and diagnostics.

Per-node configuration is defined in `src/main/resources/options.yaml` and stored in the database. `Options` resolves
these values for the active node. Examples include profile metadata, feature switches, permissions, media limits, and
Web UI behavior. Privileged options cannot be treated as ordinary user preferences because they influence resource or
server policy.
