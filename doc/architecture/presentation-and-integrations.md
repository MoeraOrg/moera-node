# Presentation and integrations

[Architecture index](../architecture.md)

## Built-in Web UI

The Web UI serves limited clients, search engines, and AI agents; the full `moera-client-react` application is external.
Controllers in [ui](../../src/main/java/org/moera/node/ui) prepare models for
[Handlebars templates](../../src/main/resources/templates). Helpers supply rendering behavior; the frontend build
only packages local static assets/styles.

`VirtualPageInterceptor`, user-agent classification, per-node policy, and UI controllers choose HTML, virtual-page
discovery, or a full-client redirect. Public HTML/media helpers must preserve permissions and resolve backend-specific
URLs correctly. [Access control](api-and-access-control.md#protect-every-representation) owns those cross-surface rules.

## Discovery and text

`PublicPageOperations` persists moment ranges for stable timeline/comment pagination, not rendered HTML.
RSS/sitemap controllers expose public output, while jobs/receptors maintain pagination and sitemap records.
`IndexNowClient` scans changed entries, submits URLs grouped by host, then records successful submission separately.
Moera Search instead consumes [peer notifications](federation.md#search-subscribers) and owns an external index.

The [text package](../../src/main/java/org/moera/node/text) converts bodies, sanitizes presentation, derives headings,
and extracts references. `LinkPreviewController` synchronously fetches metadata, directly or through linkpreview.net,
and owns preview media. Previews embedded in `Body` participate in its signature: producers use stable media references
there, leaving expiring URLs for response/rendering conversion. Editor-side preview production belongs to the client.

## Adapters

| Adapter | Node-side responsibility | Implementation |
| --- | --- | --- |
| Mail | Localized templates, recipient/verification policy, SMTP preparation | `mail/MailService` |
| FCM relay | Signed mobile push calls and client registration | `api/pushrelay/FcmRelay` |
| IndexNow | Changed-URL submission and progress marker | `indexnow/IndexNowClient` |
| linkpreview.net | Metadata retrieval for configured hosts | `linkpreviewnet/LinkPreviewNet` |
| Naming | Registered-name location/key lookup | `api/naming` |
| OCR.Space | Recognition transport; local jobs apply results | `ocrspace`, `operations/OcrJob` |
| S3 | Byte transfer/presigning; separate worker publishes metadata | `media/awss3` |

Paths are relative to [src/main/java/org/moera/node](../../src/main/java/org/moera/node). Naming/peer workflows belong
to [federation](federation.md), OCR to [media](media.md#processing), and S3 to [storage](media-storage.md#s3-migration).
[Client delivery](asynchronous-processing.md#client-delivery) owns WebSocket, SSE, FCM, and mail recovery semantics.

Deployment configures service endpoints/credentials and transport; hosted `Options` supplies node policy and identity.
Adapters do not share one uniform retry/durability contract. Their external effects are not part of a local database
transaction.
