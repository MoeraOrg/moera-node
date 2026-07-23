# Media Files in moera-node

This document gives a practical overview of how moera-node stores and serves media. It is intended for readers who are
new to the project.

## The Big Picture

Media in moera-node has two parts:

- the file itself, such as an image or a Markdown document;
- a database record describing the file and how it may be used.

The file normally lives in the directory configured by `node.media.path`. The database stores its content type, size,
image dimensions, physical filename, and other metadata.

moera-node identifies a file by a hash of its contents. Uploading the same contents twice produces the same media ID and
reuses the same `MediaFile` record. This saves disk space and lets several posts, avatars, or private-media records share
one stored file.

## The Main Records

### `MediaFile`

`MediaFile` describes a piece of content. Its ID is a Base64URL-encoded SHA-1 hash of the file. A SHA-256 digest is also
stored for uses that need a stronger digest.

The record contains:

- the MIME type and file size;
- image dimensions and orientation, when applicable;
- whether the media is public;
- the name of its local filesystem copy;
- the name of its cloud copy;
- usage information used by automatic cleanup.

The local and cloud names are nullable. If the local name is null, the media has no usable local copy. Code that needs a
local file receives `MediaFileNotAvailableException` in that case.

Cloud storage is represented in the data model but is not supported by a storage backend yet.

### `MediaFileOwner`

A `MediaFileOwner` represents a private media item belonging to the local node. It points to a `MediaFile` and adds the
information that belongs to this particular use of the content:

- a random UUID;
- a title shown to the user;
- access permissions;
- malware-related state.

Several owners can point to the same `MediaFile`. For example, the same image bytes can be uploaded under different
titles or attached in different contexts without creating extra copies on disk.

Public media is normally addressed by the `MediaFile` content hash. Private media is addressed by the
`MediaFileOwner` UUID.

### Previews

Image previews are stored as regular `MediaFile` objects. A `MediaFilePreview` links an original image to a resized
version and records its width.

When a client asks for a particular width, moera-node chooses the smallest available preview that is still large enough.
If no suitable preview exists, it serves the original.

### Leases

A `MediaLease` records that another node is allowed to use a private media item. Leases are checked when a remote node
presents a media grant.

## Where Files Are Stored

The local media directory is configured with:

```yaml
node:
  media:
    path: /var/lib/moera/media
```

A stored file is located at:

```text
<node.media.path>/<MediaFile.fileName>
```

Temporary upload data is placed under `tmp/`, and staged uploads are placed under `uploads/`, both inside the media
directory.

The configured directory must exist and be writable when the node starts.

## Uploading and Saving Media

Uploads are first streamed into a temporary file. While the data is being received, moera-node calculates:

- a SHA-1 hash used as the media ID;
- a SHA-256 digest stored with the metadata.

`MediaOperations.putInPlace()` then saves the media:

1. It takes a database lock for this media ID so that two requests cannot publish or remove the same content at the same
   time.
2. It checks whether the content is already known.
3. If a `MediaFile` already exists, it returns that record and the caller discards the new temporary copy.
4. For new content, it determines the MIME type and chooses a filename such as `<media-id>.jpg`.
5. It moves the temporary file into the media directory.
6. It saves the filename and the rest of the metadata in `media_files`.

For images, moera-node also reads the dimensions and orientation. Private image uploads may create smaller previews for
use in pages and responsive images.

Public uploads set the `exposed` flag, which allows the file to be returned by public-media endpoints. Private uploads
normally create a `MediaFileOwner` with a title and access settings.

Remote media downloaded from another node goes through the same storage and deduplication process.

## URLs: Physical Names and Web Paths

The name on disk and the name in a URL serve different purposes.

| Name | Example | Purpose |
| --- | --- | --- |
| Local filename | `<media-id>.jpg` | Locates the file under `node.media.path`. |
| Public path | `public/<media-id>.jpg` | Lets a controller find a public `MediaFile`. |
| Private path | `private/<owner-uuid>.jpg` | Lets a controller find a `MediaFileOwner`. |
| Direct path | `<local-filename>?exp=...&sig=...` | Lets the reverse proxy serve a local file directly. |

The extension in a public or private path is mainly useful to browsers. The controller uses the ID before the extension
to find the database record.

## Public Media

Public media is available through API and browser-friendly routes:

```text
/moera/api/media/public/{media-id}/data
/moera/media/public/{media-id}.{extension}
```

The controller loads the `MediaFile` by its content hash and returns it only if `exposed` is true.

Public media information returned by the API includes the MIME type, dimensions, size, normal path, and, when enabled, a
direct path.

## Private Media

Private media uses the owner UUID:

```text
/moera/api/media/private/{owner-uuid}/data
/moera/media/private/{owner-uuid}.{extension}
```

The controller loads the `MediaFileOwner`, checks access, checks malware restrictions, and then serves its `MediaFile`.
Access is allowed when the media is unrestricted, the request has an appropriate local scope, or the request includes a
valid media grant.

A media grant is a signed value that identifies the private media and its expiration time. It may also request download
behavior and provide a filename for the browser. Grants used by another node are checked against media leases.

Unauthorized private media is reported as not found. This avoids revealing whether the private item exists.

Clients may request a preview with the `width` parameter. A download response uses `Content-Disposition` and normally
gets its filename from the owner's title.

## Serving Through the Application

When a request reaches a media controller, `node.media.serve` controls how the bytes are delivered:

- `stream` — Spring streams the file from disk;
- `sendfile` — the response contains `X-SendFile` for a supporting web server;
- `accel` — the response contains `X-Accel-Redirect` for nginx.

In all three modes, moera-node still handles access checks, grants, malware checks, preview selection, MIME type, cache
headers, and download filenames. Only the final transfer of bytes is different.

For `accel`, `node.media.accel-prefix` sets the prefix used in the `X-Accel-Redirect` header.

## Direct Serving

Direct serving lets the reverse proxy return a file without sending the request through a media controller. It is
configured separately from the application serving mode:

```yaml
node:
  media:
    direct-serve:
      source: filesystem
      secret: <shared-secret>
```

The available sources are `none` and `filesystem`.

With filesystem direct serving enabled, API responses may contain a path like:

```text
<local-filename>?exp=<unix-time>&fn=<download-name>&sig=<signature>
```

The client uses this path under `/moera/media/`. The reverse proxy validates the expiration and signature with the
shared secret and serves the file from the media directory.

The signature is HMAC-SHA-256 over the media ID, expiration time, and optional browser filename. Public direct paths use
a long-lived expiration date. Private direct paths normally remain valid for three days and expire at the next UTC
midnight.

When direct serving is disabled, or when there is no local copy, the API returns only the normal controller path.

Some entry data is cached in the database. When a cached direct path is read, moera-node refreshes its expiration and
signature before returning it.

## Removing Unused Media

Database triggers track how many records refer to each `MediaFile`. When the last reference disappears, the file gets a
cleanup deadline. The current grace period is four days, which is longer than the normal lifetime of a private direct
URL.

Cleanup happens in two stages.

### Move the record to the removal queue

Every six hours, `MediaOperations.purgeUnused()` moves expired, unused media records from `media_files` to
`media_file_removals` in small batches. The removal record keeps the media ID and the names of its stored copies.

This database step does not touch the filesystem. It first makes the media unavailable to new database references while
keeping everything needed for later storage cleanup.

### Delete the stored copy

Every 30 minutes, `MediaOperations.removeMediaFiles()` processes a batch from the removal queue. Only one invocation runs
at a time in each application process.

For each item, it:

1. takes the same media-ID lock used when saving content;
2. checks whether the same content was added again after being queued for removal;
3. keeps the file if a new `MediaFile` with that ID exists;
4. otherwise deletes the recorded local file;
5. removes the queue record after cleanup succeeds.

Deleting a file that is already absent counts as success. If deletion fails, the queue record remains and a later run
tries again. This also makes cleanup safe when the application stops halfway through the operation.

Cloud deletion is not implemented yet. A queue record with a cloud filename is kept so it can be handled when a cloud
backend is added.

## Configuration Summary

| Property | Purpose |
| --- | --- |
| `node.media.path` | Root directory for local media, temporary files, and staged uploads. |
| `node.media.serve` | Application delivery mode: `stream`, `sendfile`, or `accel`. |
| `node.media.accel-prefix` | Prefix used for `X-Accel-Redirect`. |
| `node.media.direct-serve.source` | Direct serving source: `none` or `filesystem`. |
| `node.media.direct-serve.secret` | Secret shared with the direct-serving reverse proxy. |

## Where to Look in the Code

- `MediaOperations` — local files, metadata, previews, serving modes, and cleanup.
- `MediaManager` — upload and download workflows, private owners, remote media, and leases.
- `MediaUtil` — public/private paths, direct paths, signatures, and responsive-image sources.
- `MediaController` and `MediaUiController` — API and browser-facing media endpoints.
- `MediaGrantGenerator` and `MediaGrantValidator` — private-media grants.
- `MediaFile`, `MediaFileOwner`, `MediaFilePreview`, and `MediaFileRemoval` — the main database entities.

