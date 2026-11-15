# Stories and feeds

[Architecture index](../architecture.md)

## Publication ownership

A `Story` is a local publication or activity item, not the entry itself. It belongs to a node and optional feed,
may link to an entry, and owns ordering, publication attributes, read/view state, and summary metadata. A posting can
have several publications; grouped instants have parent/substory relationships.

[StoryOperations](../../src/main/java/org/moera/node/operations/StoryOperations.java) owns publication, updates,
ordering, and deletion. Feed identity is fixed at creation. Viewing permission and external source references belong
to the story independently of the entry. A picked posting's publications retain remote node/posting identity even when
access control omits the embedded posting. Picks preserve publication intent across retries and redirects.

## Visibility

An `unset` story principal derives visibility from its entry; an explicit principal controls the story independently.
A private story is local-admin-only. Reading a story does not grant access to its posting: a response may retain story
metadata while omitting the posting. Feed queries, direct reads, posting feed references, events, and public output must
use the same effective story permission.

Changing visibility can create added/updated/deleted delivery for newly permitted, continuing, and excluded readers.
Entry permission changes affect inherited stories, not explicitly controlled ones. Field-level scope requirements live
in [access control](api-and-access-control.md#protect-every-representation).

Sources: [Story](../../src/main/java/org/moera/node/data/Story.java),
[V326](../../src/main/resources/db/migration/V326__story_view_principal.sql), and
[StoryVisibilityIT](../../src/test/java/org/moera/node/data/StoryVisibilityIT.java).

## Dates and deletion

The earliest timeline publication is derived from stories. Mutations lock the posting and refresh publications before
comparing that minimum; a changed minimum emits a publication-date signal, separate from a content edit. Picked copies
retain the original receiver's publication date independently of local publications.

User-facing story deletion requires feed-editing authority. Removing the last reference also checks posting-deletion
rights, because background cleanup can later remove the unused posting. Posting locks serialize concurrent removal;
missing permission rolls back deletion. The broad-delete override bypasses only the filter guard, not authorization.
Internal deletion helpers omit this user-facing check and must be chosen deliberately.

Story deletion leaves the posting for cleanup. Stories in any feed or without a feed, and resolved links from any
retained revision, keep a normal posting linked. `PostingOperations.purgeUnlinked()` soft-deletes unused postings;
retention cleanup handles physical deletion. Media children have a separate parent lifecycle in
[media](media.md#attachments-and-child-postings).

See [StoryController](../../src/main/java/org/moera/node/rest/StoryController.java),
[PostingRepository](../../src/main/java/org/moera/node/data/PostingRepository.java), and
[StoryControllerDeleteStoriesTest](../../src/test/java/org/moera/node/rest/StoryControllerDeleteStoriesTest.java).

## Instants and derived side effects

Instant creation in `Feed.INSTANT` starts only in a liberin receptor and delegates to a dedicated
[InstantsCreator](../../src/main/java/org/moera/node/instant/InstantsCreator.java) subclass. Creators own grouping and
instant-specific policy; controllers and incoming notification handlers do not construct instants directly.

Receptors also translate publication changes into client delivery, search/sharing notifications, and contact favors.
These reactions follow the [post-commit boundary](asynchronous-processing.md#commit-boundaries); their failure does not
roll back the publication. Sharing instants retain substory-to-sharing links so removal can rebuild or delete the group.
