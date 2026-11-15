# Runtime and node contexts

[Architecture index](../architecture.md)

## Hosted identities

A local node UUID partitions tenant state. An HTTP domain selects that node; a registered Moera name identifies it to
peers and resolves through naming to a URI and signing key. These identities are not interchangeable. Repository
queries must select the intended local `node_id`; JPA does not apply a universal tenant filter.

[Domains](../../src/main/java/org/moera/node/domain/Domains.java) loads hosted nodes and their options before pending
work is restored, creating `_default_` for a fresh database.
[DomainInterceptor](../../src/main/java/org/moera/node/domain/DomainInterceptor.java) maps request hosts to this state.
Single-host mode permits fallback to the default node; multi-host mode restricts ordinary requests to configured hosts
while allowing provider provisioning on unknown hosts. Provider authority is distinct from node administration.

## Request and background context

`RequestContext` holds the selected node, request identity, URLs, scopes, and deferred completion actions.
[UniversalContext](../../src/main/java/org/moera/node/global/UniversalContext.java) delegates to it on HTTP threads
and uses thread-local state otherwise. `TaskAutowire` assigns a background task's node; `Task.run()` associates it
before execution. Global maintenance selects a node when it reaches tenant-specific work.

`UniversalContext.associate()` changes subsequent execution on the whole thread. Call it at a concrete worker/task
boundary, not inside a reusable helper. Selecting a node supplies its options and signing identity; it does not itself
grant administrator rights. Warm-domain scans exclude frozen nodes; all-domain scans intentionally include them.

## Transactions

HTTP mutations use `@Transactional` or an explicit
[Transaction](../../src/main/java/org/moera/node/util/Transaction.java) scope. That helper uses `REQUIRES_NEW` for both
reads and writes: nested scopes suspend the caller's transaction and commit independently.

JPA open-in-view is disabled. Load and convert lazy relationships inside the relevant transaction. Across asynchronous
or download boundaries, pass IDs/scalar snapshots and reload before publication. Database transactions do not make
filesystem, S3, or peer API effects atomic. The media/picker paths explicitly prepare data before final publication;
older callers can still perform external I/O inside their transactions.

Liberin completion handling and transaction-synchronized job creation are defined once in
[asynchronous processing](asynchronous-processing.md#commit-boundaries).

## Process and deployment boundary

[MoeraNodeApplication](../../src/main/java/org/moera/node/MoeraNodeApplication.java) configures MVC, schedulers, and
separate fixed-worker executors for naming, jobs, peer senders, pickers, and push. Its builder does not bound executor
queue capacity. Pool settings limit concurrent execution inside one JVM.

Domain/permission caches, job registries, sender/picker maps, and live sessions are process-local. General jobs and
per-peer workers have no database claim before every attempt. Particular row locks and S3 leases do not establish
support for multiple competing application instances on one database.

[Config](../../src/main/java/org/moera/node/config/Config.java) binds deployment settings; Spring configures HTTP,
PostgreSQL, Flyway, and SMTP. Per-node `Options` resolve stored values against descriptors/defaults from
[options.yaml](../../src/main/resources/options.yaml). Default overrides and privilege policy are handled by
`OptionsMetadata`; private profile/signing settings are not public preferences.

Startup requires a configured domain, root secret, database, and writable media root. Reverse-proxy and external-tool
requirements belong to [media storage](media-storage.md#deployment). Backups require the database and the actual media
copies it references.
