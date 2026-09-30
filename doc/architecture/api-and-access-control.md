# API and access control

## HTTP surfaces

The public Node API is rooted at `/moera/api`. Controllers under `rest` cover profiles and credentials, postings and
comments, revisions and reactions, feeds and stories, social relationships, subscriptions, moderation, media,
notifications, push delivery, and provider operations.

The protocol schema is maintained in the sibling `moeraorg.github.io` repository. Generated Java structures and the
Node API client arrive through `java-moeralib`; node code does not maintain handwritten duplicates of those wire
structures. Controller inputs call generated validation methods, while conversion helpers under `model` populate
responses, notifications, and events.

OpenAPI metadata is served under `/moera/api-docs`, with Swagger UI at `/moera/api-ui.html`.

## Authentication

`AuthenticationInterceptor` and `AuthenticationManager` populate the request's authentication state from local access
tokens, cartes delegated to another node, or the deployment root secret used for provider administration. Federation
controllers that accept signed objects verify those signatures and then authenticate the claimed node identity for the
rest of that request.

Authentication produces scopes rather than a single logged-in flag. `RequestContext` records separate local admin and
remote client scopes, the authenticated node name, ownership, friend groups, and whether the local node subscribes to
the remote node.

Incoming notification packets have their own authentication path. `NotificationController` resolves the sender's
public signing key through naming, verifies the versioned packet fingerprint, and then marks the request as signed by
that node before routing the notification.

## Authorization

Controller annotations such as `@Admin`, `@RootAdmin`, and related access annotations state endpoint requirements.
Domain objects use generated `Principal` and `PrincipalFilter` values to express visibility or mutation rights for the
public, administrators, named nodes, subscribers, and friend groups. Scope checks constrain which authenticated rights
may be used for a particular operation.

Permission data is stored with entries, reactions, media owners, friends, grants, and other protected records. API
conversion helpers apply protection before returning structures to a particular requester. Private media performs the
same checks before revealing metadata or bytes; unauthorized private media is returned as missing so its existence is
not disclosed.

## Failures and caching

Controllers translate validation, not-found, authentication, concurrency, and operation failures through
`ExceptionsControllerAdvice` into the Node API error model. Error codes and user-facing default messages are defined in
`src/main/resources/messages.properties`.

Caching annotations and `CacheControlInterceptor` select response cache policy. Some serialized attachment data is
also cached in the database. Such caches contain expiring URLs and are refreshed or invalidated by the components that
change their underlying media or storage backend.

## Built-in web interface

Controllers under `ui` render profiles, timelines, postings, people pages, RSS, sitemaps, and browser-friendly media
routes. Handlebars templates live under `src/main/resources/templates`, with helper objects discovered through
`@HelperSource`.

Per-node Web UI options decide whether these limited pages are rendered, hidden, or redirected to the full client.
Crawler detection allows server-rendered pages to remain available when interactive users are sent to the client.
