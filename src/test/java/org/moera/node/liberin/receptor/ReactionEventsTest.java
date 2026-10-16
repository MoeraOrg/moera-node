package org.moera.node.liberin.receptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.lib.node.types.notifications.PostingReactionsUpdatedNotification;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.CommentReactionAddedLiberin;
import org.moera.node.liberin.model.CommentReactionDeletedLiberin;
import org.moera.node.liberin.model.CommentReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.CommentReactionsDeletedAllLiberin;
import org.moera.node.liberin.model.PostingReactionAddedLiberin;
import org.moera.node.liberin.model.PostingReactionDeletedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionsDeletedAllLiberin;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.operations.ReactionTotalOperations;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactionEventsTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replacementEventsCarryDeletedAndAddedReactionsInOrder(boolean comment) {
        Fixture fixture = new Fixture(comment);
        Reaction deleted = fixture.reaction(false, 0x1f44d, 101L);
        Reaction added = fixture.reaction(true, 0x1f44e, 202L);

        fixture.added(added, deleted);

        assertEquals(3, fixture.events.size());
        fixture.assertReaction(0, "REACTION_DELETED", deleted);
        fixture.assertReaction(1, "REACTION_ADDED", added);
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.get(2).getType().name());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unsignedAdditionCarriesPayloadWithoutFederationNotifications(boolean comment) {
        Fixture fixture = new Fixture(comment);
        Reaction reaction = fixture.reaction(false, 0x1f44d, 101L);
        reaction.setSignature(null);

        fixture.added(reaction, null);

        assertEquals(2, fixture.events.size());
        fixture.assertReaction(0, "REACTION_ADDED", reaction);
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.get(1).getType().name());
        assertEquals(0, fixture.notifications.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replacementFiltersEachReactionAndStillRefreshesTotalsForOtherViewers(boolean comment) {
        Fixture fixture = new Fixture(comment);
        Reaction deleted = fixture.reaction(false, 0x1f44d, 101L);
        deleted.setViewPrincipal(Principal.ofNode("old-viewer"));
        Reaction added = fixture.reaction(true, 0x1f44e, 202L);
        added.setViewPrincipal(Principal.ofNode("new-viewer"));

        fixture.added(added, deleted);

        assertEquals(3, fixture.events.size());
        assertVisible(fixture.events.get(0), "old-viewer", true);
        assertVisible(fixture.events.get(0), "new-viewer", false);
        assertVisible(fixture.events.get(1), "new-viewer", true);
        assertVisible(fixture.events.get(1), "old-viewer", false);
        assertVisible(fixture.events.get(2), "totals-viewer", true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reactionEventsRespectEntryAndNegativeReactionPermissions(boolean comment) {
        Fixture fixture = new Fixture(comment);
        fixture.entry.setViewReactionsPrincipal(Principal.ofNode("viewer", "other"));
        fixture.entry.setViewNegativeReactionsPrincipal(Principal.ofNode("viewer"));
        Reaction reaction = fixture.reaction(true, 0x1f44e, 101L);
        fixture.added(reaction, null);

        assertEquals(2, fixture.events.size());
        Event event = fixture.events.getFirst();
        assertVisible(event, "viewer", true);
        assertVisible(event, "other", false);
        assertVisible(event, "anonymous", false);
        assertVisible(fixture.events.get(1), "totals-viewer", true);

        fixture.events.clear();
        fixture.posting.setViewPrincipal(Principal.ofNode("entry-viewer"));
        fixture.added(reaction, null);
        assertVisible(fixture.events.getFirst(), "viewer", false);
        assertVisible(fixture.events.get(1), "totals-viewer", false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bulkDeletionUsesNullPayloadAndPreservesTotalsDelivery(boolean comment) {
        Fixture fixture = new Fixture(comment);
        fixture.entry.setViewReactionsPrincipal(Principal.ofNode("viewer"));

        fixture.deletedAll();

        assertEquals(2, fixture.events.size());
        JsonNode json = fixture.json(0);
        assertEquals(fixture.prefix + "_REACTION_DELETED", json.get("type").asString());
        assertTrue(json.path("reaction").isNull() || json.path("reaction").isMissingNode());
        assertVisible(fixture.events.getFirst(), "viewer", true);
        assertVisible(fixture.events.getFirst(), "totals-viewer", false);
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.get(1).getType().name());
        assertVisible(fixture.events.get(1), "totals-viewer", true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingSingleReactionDoesNotReportBulkDeletion(boolean comment) {
        Fixture fixture = new Fixture(comment);

        fixture.deleted(null);

        assertEquals(1, fixture.events.size());
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.getFirst().getType().name());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void totalsOnlyUpdatesDoNotReportEntryRevisions(boolean comment) {
        Fixture fixture = new Fixture(comment);
        fixture.entry.setViewReactionsPrincipal(Principal.ADMIN);

        fixture.totalsUpdated();

        assertEquals(1, fixture.events.size());
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.getFirst().getType().name());
        assertVisible(fixture.events.getFirst(), "totals-viewer", true);
        JsonNode json = fixture.json(0);
        assertEquals(fixture.entry.getId().toString(), json.get("id").asString());
        assertFalse(json.has("reaction"));
        if (comment) {
            assertEquals(fixture.posting.getId().toString(), json.get("postingId").asString());
            assertEquals(321L, json.get("moment").asLong());
        }
    }

    private static void assertVisible(Event event, String client, boolean expected) {
        assertEquals(expected, event.getFilter().includes(false, client, false, new String[0]));
    }

    private static class Fixture {

        private final List<Event> events = new ArrayList<>();
        private final List<Notification> notifications = new ArrayList<>();
        private final Posting posting = new Posting();
        private final Comment comment;
        private final Entry entry;
        private final String prefix;
        private final PostingReactionReceptor postingReceptor = new PostingReactionReceptor() {
            @Override
            protected void send(Liberin liberin, Event event) {
                events.add(event);
            }

            @Override
            protected void send(Direction direction, Notification notification) {
                if (!(notification instanceof PostingReactionsUpdatedNotification)) {
                    notifications.add(notification);
                }
            }
        };
        private final CommentReactionReceptor commentReceptor = new CommentReactionReceptor() {
            @Override
            protected void send(Liberin liberin, Event event) {
                events.add(event);
            }

            @Override
            protected void send(Direction direction, Notification notification) {
                notifications.add(notification);
            }
        };

        private Fixture(boolean isComment) {
            posting.setId(UUID.randomUUID());
            posting.setNodeId(UUID.randomUUID());
            posting.setOwnerName("posting-author");
            revision(posting);
            if (isComment) {
                comment = new Comment();
                comment.setId(UUID.randomUUID());
                comment.setNodeId(posting.getNodeId());
                comment.setPosting(posting);
                comment.setOwnerName("comment-author");
                comment.setMoment(321L);
                revision(comment);
                entry = comment;
                prefix = "COMMENT";
            } else {
                comment = null;
                entry = posting;
                prefix = "POSTING";
            }
            UniversalContext context = mock(UniversalContext.class);
            when(context.nodeName()).thenReturn("local-node");
            ReflectionTestUtils.setField(postingReceptor, "universalContext", context);
            DirectServeOperations directServe = mock(DirectServeOperations.class);
            when(directServe.source()).thenReturn(DirectServeSource.AWSS3);
            ReflectionTestUtils.setField(postingReceptor, "directServeOperations", directServe);
            ReflectionTestUtils.setField(commentReceptor, "directServeOperations", directServe);
            ReactionTotalOperations totals = mock(ReactionTotalOperations.class);
            ReactionTotalOperations.ReactionTotalsData totalsData = mock(ReactionTotalOperations.ReactionTotalsData.class);
            when(totals.getInfo(posting)).thenReturn(totalsData);
            when(totalsData.getPublicInfo()).thenReturn(new ReactionTotalsInfo());
            ReflectionTestUtils.setField(postingReceptor, "reactionTotalOperations", totals);
        }

        private void revision(Entry target) {
            EntryRevision revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(target);
            target.setCurrentRevision(revision);
        }

        private Reaction reaction(boolean negative, int emoji, long moment) {
            Reaction reaction = new Reaction();
            reaction.setId(UUID.randomUUID());
            reaction.setEntryRevision(entry.getCurrentRevision());
            reaction.setOwnerName("reaction-owner");
            reaction.setOwnerFullName("Reaction Owner");
            reaction.setOwnerSourceUri("https://example.org/owner");
            reaction.setOwnerGender("other");
            MediaFile avatar = new MediaFile();
            avatar.setId("avatar-id");
            avatar.setFileName("avatar.png");
            avatar.setMimeType("image/png");
            avatar.setSizeX(80);
            avatar.setSizeY(80);
            reaction.setOwnerAvatarMediaFile(avatar);
            reaction.setOwnerAvatarShape("circle");
            reaction.setNegative(negative);
            reaction.setEmoji(emoji);
            reaction.setMoment(moment);
            reaction.setSignature(new byte[]{1, 2, 3});
            reaction.setSignatureVersion((short) 1);
            return reaction;
        }

        private void added(Reaction added, Reaction deleted) {
            if (comment != null) {
                var liberin = new CommentReactionAddedLiberin(comment);
                liberin.setAddedReaction(added);
                liberin.setDeletedReaction(deleted);
                commentReceptor.added(liberin);
            } else {
                var liberin = new PostingReactionAddedLiberin(posting);
                liberin.setAddedReaction(added);
                liberin.setDeletedReaction(deleted);
                liberin.setReactionTotals(new ReactionTotalsInfo());
                postingReceptor.added(liberin);
            }
        }

        private void deleted(Reaction reaction) {
            if (comment != null) {
                var liberin = new CommentReactionDeletedLiberin(comment);
                liberin.setReaction(reaction);
                commentReceptor.deleted(liberin);
            } else {
                var liberin = new PostingReactionDeletedLiberin(posting);
                liberin.setReaction(reaction);
                liberin.setReactionTotals(new ReactionTotalsInfo());
                postingReceptor.deleted(liberin);
            }
        }

        private void deletedAll() {
            if (comment != null) {
                commentReceptor.deletedAll(new CommentReactionsDeletedAllLiberin(comment));
            } else {
                postingReceptor.deletedAll(new PostingReactionsDeletedAllLiberin(posting));
            }
        }

        private void totalsUpdated() {
            if (comment != null) {
                commentReceptor.totalsUpdated(new CommentReactionTotalsUpdatedLiberin(comment));
            } else {
                postingReceptor.totalsUpdated(new PostingReactionTotalsUpdatedLiberin(posting, new ReactionTotalsInfo()));
            }
        }

        private JsonNode json(int index) {
            return new ObjectMapper().valueToTree(events.get(index));
        }

        private void assertReaction(int index, String suffix, Reaction reaction) {
            JsonNode json = json(index);
            assertEquals(prefix + "_" + suffix, json.get("type").asString());
            assertEquals(entry.getId().toString(), json.get("id").asString());
            if (comment != null) {
                assertEquals(posting.getId().toString(), json.get("postingId").asString());
                assertEquals(321L, json.get("moment").asLong());
            }
            JsonNode payload = json.get("reaction");
            assertEquals("reaction-owner", payload.get("ownerName").asString());
            assertEquals("Reaction Owner", payload.get("ownerFullName").asString());
            assertEquals("https://example.org/owner", payload.get("ownerSourceUri").asString());
            assertEquals("other", payload.get("ownerGender").asString());
            assertEquals("avatar-id", payload.get("ownerAvatar").get("mediaId").asString());
            assertEquals("circle", payload.get("ownerAvatar").get("shape").asString());
            assertEquals(reaction.isNegative(), payload.get("negative").asBoolean());
            assertEquals(reaction.getEmoji(), payload.get("emoji").asInt());
            assertEquals(reaction.getMoment(), payload.get("moment").asLong());
            assertEquals(posting.getId().toString(), payload.get("postingId").asString());
            if (comment != null) {
                assertEquals(comment.getId().toString(), payload.get("commentId").asString());
            }
            assertFalse(payload.path("operations").isNull());
            for (String property : List.of("ownerOperations", "seniorOperations", "majorOperations")) {
                assertTrue(payload.path(property).isNull() || payload.path(property).isMissingNode());
            }
            if (reaction.getSignature() != null) {
                assertEquals("\"AQID\"", payload.get("signature").toString());
                assertEquals(1, payload.get("signatureVersion").asInt());
            }
            assertEquals(json.toString(), new ObjectMapper().valueToTree(events.get(index).copy()).toString());
            assertFalse(json.has("posting"));
        }

    }

}
