package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.notifications.Notification;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.CommentReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.receptor.CommentReactionReceptor;
import org.moera.node.liberin.receptor.PostingReactionReceptor;
import org.moera.node.model.event.Event;
import org.moera.node.notification.send.Direction;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactionMutationEventsTest {

    @Test
    void unsignedReactionCreationReportsTheStoredReaction() {
        Fixture fixture = new Fixture(false);
        when(fixture.repository.save(any(Reaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReactionDescription description = new ReactionDescription();
        description.setOwnerName("reactor");
        description.setEmoji(0x1f44d);
        List<Reaction> added = new ArrayList<>();

        Reaction reaction = fixture.operations.post(description, fixture.entry, null, added::add);

        assertEquals(List.of(reaction), added);
        assertNull(reaction.getSignature());
        assertSame(fixture.entry.getCurrentRevision(), reaction.getEntryRevision());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void expirationReportsDeletionAndRestoredReactionWithoutFederationPointNotifications(boolean comment) {
        Fixture fixture = new Fixture(comment);
        Reaction expired = fixture.reaction(100L);
        Reaction restored = fixture.reaction(50L);
        restored.setDeletedAt(Timestamp.from(Instant.now().minusSeconds(60)));
        restored.setReplaced(true);
        restored.setSignature(new byte[]{1});
        when(fixture.repository.findExpired(any())).thenReturn(List.of(expired));
        when(fixture.repository.findReplacedByEntryIdAndOwner(eq(fixture.entry.getId()), eq("reactor"), any()))
            .thenReturn(List.of(restored));

        fixture.operations.purgeExpired();

        assertEquals(3, fixture.events.size());
        fixture.assertEvent(0, "REACTION_DELETED", 100L);
        fixture.assertEvent(1, "REACTION_ADDED", 50L);
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.get(2).getType().name());
        assertNull(restored.getDeletedAt());
        assertNull(restored.getDeadline());
        assertFalse(restored.isReplaced());
        verify(fixture.repository).delete(expired);
        verify(fixture.totals).changeTotals(fixture.entry, restored, 1);
        verify(fixture.totals).changeTotals(fixture.entry, expired, -1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void expirationWithoutReplacementDeletesOnlyTheExpiredReaction(boolean comment) {
        Fixture fixture = new Fixture(comment);
        Reaction expired = fixture.reaction(100L);
        when(fixture.repository.findExpired(any())).thenReturn(List.of(expired));

        fixture.operations.purgeExpired();

        assertEquals(2, fixture.events.size());
        fixture.assertEvent(0, "REACTION_DELETED", 100L);
        assertEquals(fixture.prefix + "_REACTIONS_CHANGED", fixture.events.get(1).getType().name());
    }

    @Test
    void purgingAnAlreadyDeletedReactionDoesNotAnnounceAnotherDeletion() {
        Fixture fixture = new Fixture(false);
        Reaction expired = fixture.reaction(100L);
        expired.setDeletedAt(Timestamp.from(Instant.now().minusSeconds(60)));
        when(fixture.repository.findExpired(any())).thenReturn(List.of(expired));

        fixture.operations.purgeExpired();

        assertEquals(0, fixture.events.size());
        verify(fixture.repository).delete(expired);
    }

    private static class Fixture {

        private final ReactionOperations operations = new ReactionOperations();
        private final ReactionRepository repository = mock(ReactionRepository.class);
        private final ReactionTotalOperations totals = mock(ReactionTotalOperations.class);
        private final List<Event> events = new ArrayList<>();
        private final Entry entry;
        private final String prefix;

        private Fixture(boolean isComment) {
            Posting posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(UUID.randomUUID());
            posting.setOwnerName("posting-owner");
            if (isComment) {
                Comment comment = new Comment();
                comment.setId(UUID.randomUUID());
                comment.setNodeId(posting.getNodeId());
                comment.setOwnerName("comment-owner");
                comment.setPosting(posting);
                entry = comment;
                prefix = "COMMENT";
            } else {
                entry = posting;
                prefix = "POSTING";
            }
            EntryRevision revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(entry);
            entry.setCurrentRevision(revision);
            when(totals.getInfo(entry)).thenReturn(new ReactionTotalOperations.ReactionTotalsData(null, entry, List.of()));
            LiberinManager manager = mock(LiberinManager.class);
            doAnswer(invocation -> {
                Liberin liberin = invocation.getArgument(0);
                assertEquals(entry.getNodeId(), liberin.getNodeId());
                if (liberin instanceof PostingReactionTotalsUpdatedLiberin postingLiberin) {
                    postingReceptor().totalsUpdated(postingLiberin);
                } else if (liberin instanceof CommentReactionTotalsUpdatedLiberin commentLiberin) {
                    commentReceptor().totalsUpdated(commentLiberin);
                }
                return null;
            }).when(manager).send(any(Liberin.class));
            ReflectionTestUtils.setField(operations, "reactionRepository", repository);
            ReflectionTestUtils.setField(operations, "reactionTotalOperations", totals);
            ReflectionTestUtils.setField(operations, "liberinManager", manager);
            ReflectionTestUtils.setField(operations, "requestCounter", new RequestCounter());
            ReflectionTestUtils.setField(operations, "tx", new Transaction());
        }

        private PostingReactionReceptor postingReceptor() {
            return new PostingReactionReceptor() {
                @Override
                protected void send(Liberin liberin, Event event) {
                    events.add(event);
                }

                @Override
                protected void send(Direction direction, Notification notification) {
                    assertEquals("PostingReactionsUpdatedNotification", notification.getClass().getSimpleName());
                }
            };
        }

        private CommentReactionReceptor commentReceptor() {
            return new CommentReactionReceptor() {
                @Override
                protected void send(Liberin liberin, Event event) {
                    events.add(event);
                }

                @Override
                protected void send(Direction direction, Notification notification) {
                    throw new AssertionError("Expiration must not send federation point notifications");
                }
            };
        }

        private Reaction reaction(long moment) {
            Reaction reaction = new Reaction();
            reaction.setId(UUID.randomUUID());
            reaction.setEntryRevision(entry.getCurrentRevision());
            reaction.setOwnerName("reactor");
            reaction.setEmoji(0x1f44d);
            reaction.setMoment(moment);
            reaction.setDeadline(Timestamp.from(Instant.now().minusSeconds(1)));
            return reaction;
        }

        private void assertEvent(int index, String type, long moment) {
            var json = new ObjectMapper().valueToTree(events.get(index));
            assertEquals(prefix + "_" + type, json.get("type").asString());
            assertEquals("reactor", json.get("reaction").get("ownerName").asString());
            assertEquals(moment, json.get("reaction").get("moment").asLong());
        }

    }

}
