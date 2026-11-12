package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.ReactionTotal;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.RequestContextImpl;
import org.moera.node.liberin.model.CommentDeletedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.CommentOperations;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.ReactionOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentAndReactionDeleteAuthorizationTest {

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("authorizationCases")
    void clientAndAdminRightsUseTheirRespectiveOwners(Target target, Authorization authorization) {
        Fixture fixture = new Fixture(target, authorization.owner());
        fixture.requestContext.setClientName(authorization.client());
        fixture.requestContext.setClientScope(authorization.clientScope().getMask());
        fixture.requestContext.setAdminScope(authorization.adminScope().getMask());

        fixture.assertDeletion(authorization.allowed());
    }

    private static Stream<Arguments> authorizationCases() {
        return Stream.of(
            new Authorization("alice_0", Scope.DELETE_OWN_CONTENT, Scope.NONE, "alice_0", true),
            new Authorization("alice_0", Scope.NONE, Scope.DELETE_OWN_CONTENT, "alice_0", true),
            new Authorization("alice_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "alice_0", false),
            new Authorization("alice_0", Scope.NONE, Scope.DELETE_OWN_CONTENT, "bob_0", false),
            new Authorization("alice_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "bob_0", true),
            new Authorization("alice_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "carol_0", true),
            new Authorization("bob_0", Scope.DELETE_OWN_CONTENT, Scope.NONE, "bob_0", true),
            new Authorization("bob_0", Scope.DELETE_OTHERS_CONTENT, Scope.NONE, "bob_0", false),
            new Authorization("bob_0", Scope.DELETE_OWN_CONTENT, Scope.NONE, "alice_0", false),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OWN_CONTENT, "alice_0", true),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "alice_0", false),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OWN_CONTENT, "bob_0", false),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "bob_0", true),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "carol_0", true),
            new Authorization("bob_0", Scope.NONE, Scope.DELETE_OWN_CONTENT, "carol_0", false),
            new Authorization("bob_0", Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT, "alice_0", false),
            new Authorization("bob_0", Scope.DELETE_OTHERS_CONTENT, Scope.DELETE_OWN_CONTENT, "alice_0", true),
            new Authorization(null, Scope.NONE, Scope.DELETE_OWN_CONTENT, "alice_0", true),
            new Authorization(null, Scope.NONE, Scope.DELETE_OTHERS_CONTENT, "carol_0", true)
        ).flatMap(authorization -> Arrays.stream(Target.values()).map(target -> Arguments.of(target, authorization)));
    }

    @ParameterizedTest
    @EnumSource(Target.class)
    void postingAuthorNeedsClientScopeForDeletingOthersContent(Target target) {
        Fixture fixture = new Fixture(target, "carol_0");
        fixture.requestContext.setClientName(fixture.posting.getOwnerName());
        fixture.requestContext.setClientScope(Scope.DELETE_OWN_CONTENT.getMask());
        fixture.assertDeletion(false);

        fixture.requestContext.setClientScope(Scope.DELETE_OTHERS_CONTENT.getMask());
        fixture.assertDeletion(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void commentAuthorNeedsClientScopeForDeletingOthersReactions(boolean ownScope) {
        Fixture fixture = new Fixture(Target.COMMENT_REACTION, "carol_0");
        fixture.requestContext.setClientName(fixture.comment.getOwnerName());
        fixture.requestContext.setClientScope(
            (ownScope ? Scope.DELETE_OWN_CONTENT : Scope.DELETE_OTHERS_CONTENT).getMask()
        );

        fixture.assertDeletion(!ownScope);
    }

    @ParameterizedTest
    @EnumSource(Target.class)
    void effectiveDeletePrincipalStillRestrictsAdministrators(Target target) {
        Fixture fixture = new Fixture(target, "carol_0");
        fixture.requestContext.setAdminScope(Scope.ALL.getMask());
        fixture.comment.setParentDeletePrincipal(Principal.NONE);
        fixture.reaction.setPostingDeletePrincipal(Principal.NONE);

        fixture.assertDeletion(false);
    }

    private record Authorization(String client, Scope clientScope, Scope adminScope, String owner, boolean allowed) {
    }

    private enum Target {
        COMMENT,
        POSTING_REACTION,
        COMMENT_REACTION
    }

    private static class Fixture {

        private final Target target;
        private final RequestContextImpl requestContext = new RequestContextImpl();
        private final Posting posting = new Posting();
        private final Comment comment = new Comment();
        private final Reaction reaction = new Reaction();
        private final ReactionTotal entryTotal = new ReactionTotal();
        private final ReactionTotal revisionTotal = new ReactionTotal();
        private final CommentController commentController = new CommentController();
        private final ReactionOperations reactionOperations = new ReactionOperations();
        private final ReactionRepository reactionRepository = mock(ReactionRepository.class);
        private final EntityManager entityManager = mock(EntityManager.class);
        private final MediaOperations mediaOperations = mock(MediaOperations.class);
        private final List<Reaction> deletedReactions = new ArrayList<>();

        private Fixture(Target target, String owner) {
            this.target = target;
            UUID nodeId = UUID.randomUUID();
            Options options = mock(Options.class);
            when(options.nodeId()).thenReturn(nodeId);
            when(options.nodeName()).thenReturn("alice_0");
            when(options.getDuration("comment.deleted.lifetime")).thenReturn(ExtendedDuration.NEVER);
            when(options.getDuration("reaction.deleted.lifetime")).thenReturn(ExtendedDuration.NEVER);
            requestContext.setOptions(options);
            posting.setOwnerName("david_0");
            posting.setTotalChildren(1);
            comment.setPosting(posting);
            comment.setOwnerName(target == Target.COMMENT ? owner : "eve_0");
            for (Entry entry : List.of(posting, comment)) {
                entry.setId(UUID.randomUUID());
                entry.setNodeId(nodeId);
                EntryRevision revision = new EntryRevision();
                revision.setId(UUID.randomUUID());
                revision.setEntry(entry);
                entry.setCurrentRevision(revision);
            }
            reaction.setId(UUID.randomUUID());
            reaction.setOwnerName(owner);
            reaction.setEntryRevision((target == Target.POSTING_REACTION ? posting : comment).getCurrentRevision());
            reaction.setEmoji(0x1f44d);
            entryTotal.setTotal(1);
            revisionTotal.setTotal(1);

            if (target == Target.COMMENT) {
                CommentRepository commentRepository = mock(CommentRepository.class);
                when(commentRepository.findFullByNodeIdAndId(nodeId, comment.getId()))
                    .thenReturn(Optional.of(comment));
                CommentOperations commentOperations = new CommentOperations();
                ReflectionTestUtils.setField(commentOperations, "requestContext", requestContext);
                ReflectionTestUtils.setField(commentOperations, "mediaOperations", mediaOperations);
                ReflectionTestUtils.setField(commentOperations, "postingOperations", new PostingOperations());
                ReflectionTestUtils.setField(commentController, "requestContext", requestContext);
                ReflectionTestUtils.setField(commentController, "commentRepository", commentRepository);
                ReflectionTestUtils.setField(commentController, "commentOperations", commentOperations);
                ReflectionTestUtils.setField(commentController, "entityManager", entityManager);
                ReflectionTestUtils.setField(commentController, "favorOperations", mock(FavorOperations.class));
                ReflectionTestUtils.setField(
                    commentController, "blockedUserOperations", mock(BlockedUserOperations.class)
                );
            } else {
                Entry entry = reaction.getEntryRevision().getEntry();
                when(reactionRepository.findByEntryIdAndOwner(entry.getId(), owner)).thenReturn(reaction);
                ReactionTotalRepository reactionTotalRepository = mock(ReactionTotalRepository.class);
                when(reactionTotalRepository.findByEntryId(entry.getId(), false, reaction.getEmoji()))
                    .thenReturn(entryTotal);
                when(reactionTotalRepository.findByEntryRevisionId(
                    entry.getCurrentRevision().getId(), false, reaction.getEmoji()
                )).thenReturn(revisionTotal);
                ReactionTotalOperations reactionTotalOperations = new ReactionTotalOperations();
                ReflectionTestUtils.setField(
                    reactionTotalOperations, "reactionTotalRepository", reactionTotalRepository
                );
                ReflectionTestUtils.setField(reactionOperations, "requestContext", requestContext);
                ReflectionTestUtils.setField(reactionOperations, "reactionRepository", reactionRepository);
                ReflectionTestUtils.setField(reactionOperations, "reactionTotalOperations", reactionTotalOperations);
            }
        }

        private void delete() {
            if (target == Target.COMMENT) {
                commentController.delete(posting.getId().toString(), comment.getId().toString());
            } else {
                reactionOperations.delete(
                    reaction.getOwnerName(), reaction.getEntryRevision().getEntry(), deletedReactions::add
                );
            }
        }

        private void assertDeletion(boolean allowed) {
            if (allowed) {
                assertDoesNotThrow(this::delete);
            } else {
                assertThrows(AuthenticationException.class, this::delete);
                verifyNoInteractions(entityManager, mediaOperations);
                verify(reactionRepository, never()).flush();
            }
            if (target == Target.COMMENT) {
                assertEquals(allowed, comment.getDeletedAt() != null);
                assertEquals(allowed, comment.getCurrentRevision().getDeletedAt() != null);
                assertEquals(allowed ? 0 : 1, posting.getTotalChildren());
                assertEquals(allowed ? List.of(comment) : List.of(), requestContext.getAfterCommitLiberins().stream()
                    .map(CommentDeletedLiberin.class::cast)
                    .map(CommentDeletedLiberin::getComment)
                    .toList());
            } else {
                if (allowed) {
                    assertNotNull(reaction.getDeletedAt());
                } else {
                    assertNull(reaction.getDeletedAt());
                }
                assertEquals(allowed ? 0 : 1, entryTotal.getTotal());
                assertEquals(allowed ? 0 : 1, revisionTotal.getTotal());
                assertEquals(allowed ? List.of(reaction) : List.of(), deletedReactions);
            }
        }

    }

}
