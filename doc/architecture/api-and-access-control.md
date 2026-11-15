# API and access control

[Architecture index](../architecture.md)

## Protocol boundary

Controllers under [rest](../../src/main/java/org/moera/node/rest) expose `/moera/api`. Generated moeralib structures
and validation represent the public contract; handwritten [model helpers](../../src/main/java/org/moera/node/model)
convert between it and stored entities. Wire changes start in the sibling specifications listed in the architecture
index. Error mapping belongs to `ExceptionsControllerAdvice` and `messages.properties`.

## Authentication and authorization

[AuthenticationInterceptor](../../src/main/java/org/moera/node/auth/AuthenticationInterceptor.java) establishes
request identity and enforces endpoint annotations. `AuthenticationManager` validates tokens and cartes:

| Credential | Authority and limit |
| --- | --- |
| Local token | Node administrator scopes, constrained by expiry and optional IP binding |
| Carte | Signed peer identity/scopes, constrained by target/time/address checks and delegated grants |
| Root secret | Deployment/provider administration, distinct from hosted-node permissions |
| Signed content/notification | Claimed identity becomes trusted only after the receiving controller verifies it |

Tokens are stored as SHA-256 digests; only creation returns the full secret. Later output uses a stored prefix.
For REST cartes, the local owner's declared admin scope applies directly and gains view permissions from client scope;
other owners' admin scopes are intersected with local grants. WebSocket authentication is a separate path in
`EventManager` and must be reviewed alongside HTTP changes.

Authorization requires both an object's effective principal and credential scope. Principals express public, admin,
named-node, subscriber, and friend-group rights. Effective permissions can inherit parent or receiver policy.
`isPrincipal(principal, clientScope, adminScope)` keeps client and administrative rights distinct; ownership alone
must not replace this check. For deletion, own-content and others-content scopes depend on the acting role and author,
while the effective delete principal still applies.

See [UniversalContext](../../src/main/java/org/moera/node/global/UniversalContext.java) and the
[deletion authorization tests](../../src/test/java/org/moera/node/rest/CommentAndReactionDeleteAuthorizationTest.java).

## Protect every representation

A successful lookup is not the end of authorization. Preserve object and field permissions when producing:

- REST content, operations, reactions, and feed references;
- linked-posting expansions, without traversing inaccessible intermediate targets;
- events and peer notifications, including recipients losing access after a permission change;
- public HTML, RSS, sitemaps, and media metadata/bytes.

A story and its entry have independent access decisions; [stories and feeds](stories-and-feeds.md#visibility) owns
that rule. Story external sources require administrative `VIEW_CONTENT`, separately from feed flags under `VIEW_FEEDS`.
Push payloads omit those sources. Event payloads need per-recipient protection without mutating replay state; see
[client delivery](asynchronous-processing.md#client-delivery). Media grants and direct URLs have their own
[serving boundary](media-storage.md#serving).

Response caching must preserve these decisions. Serialized attachment caches are an optimization, not permission
authority: conversion supplies requester-specific grants/URLs and refreshes expired paths. Mutation owners invalidate
caches when their inputs change. HTTP cache policy itself belongs to `CacheControlInterceptor`.
