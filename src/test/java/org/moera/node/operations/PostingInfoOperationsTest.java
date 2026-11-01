package org.moera.node.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.userlist.SheriffUserListOperations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostingInfoOperationsTest {

    private final UUID nodeId = UUID.randomUUID();
    private final PostingInfoOperations operations = new PostingInfoOperations();
    private final RequestContext requestContext = mock(RequestContext.class);
    private final LinkedEntryRepository linkedEntryRepository = mock(LinkedEntryRepository.class);
    private final PostingRepository postingRepository = mock(PostingRepository.class);
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final EntryOperations entryOperations = mock(EntryOperations.class);
    private final ReactionRepository reactionRepository = mock(ReactionRepository.class);
    private final OwnReactionRepository ownReactionRepository = mock(OwnReactionRepository.class);
    private final BlockedUserOperations blockedUserOperations = mock(BlockedUserOperations.class);
    private final BlockedByUserOperations blockedByUserOperations = mock(BlockedByUserOperations.class);
    private final SheriffUserListOperations sheriffUserListOperations = mock(SheriffUserListOperations.class);
    private final List<LinkedEntry> links = new ArrayList<>();
    private final List<Posting> targets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(requestContext.nodeName()).thenReturn("local_0");
        when(requestContext.isPrincipal(any(), any())).thenReturn(true);
        when(linkedEntryRepository.findResolvedByRevisionIds(eq(nodeId), any())).thenAnswer(invocation -> {
            Collection<UUID> revisionIds = invocation.getArgument(1);
            return links.stream().filter(link -> revisionIds.contains(link.getEntryRevision().getId())).toList();
        });
        when(postingRepository.findFullByNodeIdAndIds(eq(nodeId), any())).thenAnswer(invocation -> {
            Collection<UUID> postingIds = invocation.getArgument(1);
            return targets.stream().filter(posting -> postingIds.contains(posting.getId())).toList();
        });
        when(entryOperations.prepareMediaAttachments(any())).thenReturn(MediaAttachmentsProvider.NONE);
        ReflectionTestUtils.setField(operations, "requestContext", requestContext);
        ReflectionTestUtils.setField(operations, "linkedEntryRepository", linkedEntryRepository);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(operations, "entryOperations", entryOperations);
        ReflectionTestUtils.setField(operations, "directServeOperations", mock(DirectServeOperations.class));
        ReflectionTestUtils.setField(operations, "reactionRepository", reactionRepository);
        ReflectionTestUtils.setField(operations, "ownReactionRepository", ownReactionRepository);
        ReflectionTestUtils.setField(operations, "blockedUserOperations", blockedUserOperations);
        ReflectionTestUtils.setField(operations, "blockedByUserOperations", blockedByUserOperations);
        ReflectionTestUtils.setField(operations, "sheriffUserListOperations", sheriffUserListOperations);
    }

    @Test
    void feedTargetsAreLoadedOnceAndReceiveClientReactionsAndBlockings() {
        when(requestContext.getClientName(Scope.IDENTIFY)).thenReturn("reader_0");
        PostingInfo first = source();
        PostingInfo second = source();
        Posting shared = target();
        Posting another = target();
        Posting deeper = target();
        link(first, shared);
        link(second, shared);
        link(second, another);
        link(source(shared), deeper);
        Reaction reaction = reaction(shared, Principal.PUBLIC, 123);
        when(reactionRepository.findByEntryIdsAndOwner(any(), eq("reader_0")))
            .thenReturn(List.of(reaction, reaction(deeper, Principal.PUBLIC, 456)));
        BlockedUser global = new BlockedUser();
        global.setBlockedOperation(BlockedOperation.COMMENT);
        BlockedUser individual = new BlockedUser();
        individual.setEntry(shared);
        individual.setBlockedOperation(BlockedOperation.REACTION);
        when(blockedUserOperations.search(
            eq(nodeId), any(), eq("reader_0"), anyCollection(), isNull(), isNull(), eq(false)
        )).thenReturn(List.of(global, individual));
        doAnswer(invocation -> {
            Collection<PostingInfo> postings = invocation.getArgument(0);
            SheriffMark mark = new SheriffMark();
            mark.setSheriffName("sheriff_0");
            postings.forEach(posting -> posting.setSheriffMarks(List.of(mark)));
            return null;
        }).when(sheriffUserListOperations).fillSheriffListMarks(anyCollection());

        operations.fillLinkedPostings(List.of(first, second));

        PostingInfo sharedInfo = first.getLinkedPostings().getFirst();
        assertNotSame(sharedInfo, second.getLinkedPostings().getFirst());
        assertEquals(123, sharedInfo.getClientReaction().getEmoji());
        assertEquals(List.of("addComment", "addReaction"), sharedInfo.getBlockedOperations());
        assertEquals(List.of("addReaction"), sharedInfo.getBlockedCommentOperations());
        assertEquals(List.of("addComment"), second.getLinkedPostings().get(1).getBlockedOperations());
        PostingInfo deeperInfo = sharedInfo.getLinkedPostings().getFirst();
        assertEquals(deeper.getId().toString(), deeperInfo.getId());
        assertEquals(456, deeperInfo.getClientReaction().getEmoji());
        assertEquals(List.of("addComment"), deeperInfo.getBlockedOperations());
        assertEquals("sheriff_0", deeperInfo.getSheriffMarks().getFirst().getSheriffName());
        assertTrue(deeperInfo.getLinkedPostings().isEmpty());
        verify(linkedEntryRepository, times(3)).findResolvedByRevisionIds(eq(nodeId), any());
        verify(postingRepository).findFullByNodeIdAndIds(nodeId, Set.of(shared.getId(), another.getId()));
        verify(postingRepository).findFullByNodeIdAndIds(nodeId, Set.of(deeper.getId()));
        verify(storyRepository).findByEntryIds(eq(nodeId), any());
        verify(entryOperations).prepareMediaAttachments(any());
        verify(reactionRepository).findByEntryIdsAndOwner(any(), eq("reader_0"));
        verify(sheriffUserListOperations).fillSheriffListMarks(anyCollection());
    }

    @Test
    void privateReactionRequiresContentScope() {
        when(requestContext.getClientName(Scope.IDENTIFY)).thenReturn("reader_0");
        PostingInfo source = source();
        Posting target = target();
        link(source, target);
        when(reactionRepository.findByEntryIdsAndOwner(any(), any()))
            .thenReturn(List.of(reaction(target, Principal.PRIVATE, 123)));

        operations.withLinkedPostings(source);
        assertNull(source.getLinkedPostings().getFirst().getClientReaction());

        when(requestContext.hasClientScope(Scope.VIEW_CONTENT)).thenReturn(true);
        operations.withLinkedPostings(source);
        assertEquals(123, source.getLinkedPostings().getFirst().getClientReaction().getEmoji());
    }

    @Test
    void remoteReactionsAndBlockingsUseBothNodeNameAndPostingId() {
        when(requestContext.isOwner()).thenReturn(true);
        when(requestContext.isAdmin(Scope.VIEW_CONTENT)).thenReturn(true);
        when(requestContext.isAdmin(Scope.VIEW_PEOPLE)).thenReturn(true);
        PostingInfo source = source();
        Posting first = remoteTarget("first_0");
        Posting second = remoteTarget("second_0");
        link(source, first);
        link(source, second);
        when(ownReactionRepository.findAllByRemotePostingIds(nodeId, List.of("same-id")))
            .thenReturn(List.of(ownReaction("first_0", 123), ownReaction("second_0", 456)));
        BlockedByUser global = new BlockedByUser();
        global.setRemoteNodeName("first_0");
        global.setBlockedOperation(BlockedOperation.COMMENT);
        BlockedByUser individual = new BlockedByUser();
        individual.setRemoteNodeName("second_0");
        individual.setRemotePostingId("same-id");
        individual.setBlockedOperation(BlockedOperation.REACTION);
        when(blockedByUserOperations.search(eq(nodeId), anyList(), anyList(), anyBoolean()))
            .thenReturn(List.of(global, individual));

        operations.withLinkedPostings(source);

        assertEquals(123, source.getLinkedPostings().getFirst().getClientReaction().getEmoji());
        assertEquals(456, source.getLinkedPostings().get(1).getClientReaction().getEmoji());
        assertEquals(List.of("addComment"), source.getLinkedPostings().getFirst().getBlockedOperations());
        assertEquals(List.of("addReaction"), source.getLinkedPostings().get(1).getBlockedOperations());
        verify(ownReactionRepository).findAllByRemotePostingIds(nodeId, List.of("same-id"));
        verify(blockedByUserOperations).search(eq(nodeId), anyList(), anyList(), eq(false));
    }

    @Test
    void ownerWithoutRequiredScopesDoesNotReceiveRemoteClientMetadata() {
        when(requestContext.isOwner()).thenReturn(true);
        PostingInfo source = source();
        link(source, remoteTarget("remote_0"));

        operations.withLinkedPostings(source);

        assertNull(source.getLinkedPostings().getFirst().getClientReaction());
        assertNull(source.getLinkedPostings().getFirst().getBlockedOperations());
        verifyNoInteractions(reactionRepository, ownReactionRepository, blockedUserOperations, blockedByUserOperations);
    }

    @Test
    void anonymousClientDoesNotTriggerClientMetadataLookups() {
        PostingInfo source = source();
        link(source, target());

        operations.withLinkedPostings(source);

        verifyNoInteractions(reactionRepository, ownReactionRepository, blockedUserOperations, blockedByUserOperations);
    }

    @Test
    void sliceWithoutLinksDoesNotLoadTargets() {
        PostingInfo first = source();
        PostingInfo second = source();

        operations.fillLinkedPostings(List.of(first, second));

        assertTrue(first.getLinkedPostings().isEmpty());
        assertTrue(second.getLinkedPostings().isEmpty());
        verifyNoInteractions(postingRepository, storyRepository, entryOperations, reactionRepository,
            ownReactionRepository, blockedUserOperations, blockedByUserOperations, sheriffUserListOperations);
    }

    @Test
    void selfReferenceIsOmittedWithoutHidingTargetFromOtherPostings() {
        Posting target = target();
        PostingInfo self = source(target);
        PostingInfo other = source();
        link(self, target);
        link(other, target);

        operations.fillLinkedPostings(List.of(self, other));

        assertTrue(self.getLinkedPostings().isEmpty());
        assertEquals(List.of(self.getId()), other.getLinkedPostings().stream().map(PostingInfo::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void circularReferencesProduceFiniteSinglePostingAndSliceResponses(boolean slice) throws IOException {
        Posting a = target();
        Posting b = target();
        Posting c = target();
        PostingInfo first = source(a);
        PostingInfo second = source(b);
        PostingInfo third = source(c);
        link(first, b);
        link(second, c);
        link(third, a);
        var roots = slice ? List.of(first, second, third) : List.of(first);

        if (slice) {
            operations.fillLinkedPostings(roots);
        } else {
            operations.withLinkedPostings(first);
        }

        var all = List.of(first, second, third);
        MockHttpOutputMessage response = new MockHttpOutputMessage();
        new JacksonJsonHttpMessageConverter().write(slice ? roots : first, MediaType.APPLICATION_JSON, response);
        var json = new ObjectMapper().readTree(response.getBodyAsString());
        for (int i = 0; i < roots.size(); i++) {
            var current = roots.get(i);
            var currentJson = slice ? json.get(i) : json;
            for (int depth = 1; depth <= 2; depth++) {
                assertNotNull(current.getLinkedPostings());
                assertEquals(1, current.getLinkedPostings().size());
                current = current.getLinkedPostings().getFirst();
                currentJson = currentJson.at("/linkedPostings/0");
                PostingInfo expected = all.get((i + depth) % 3);
                assertEquals(expected.getId(), current.getId());
                assertNotSame(expected, current);
                assertEquals(expected.getId(), currentJson.path("id").asString());
            }
            assertTrue(current.getLinkedPostings().isEmpty());
            assertTrue(currentJson.path("linkedPostings").isEmpty());
        }
        verify(linkedEntryRepository, times(slice ? 1 : 3)).findResolvedByRevisionIds(eq(nodeId), any());
        verify(postingRepository, times(slice ? 1 : 3)).findFullByNodeIdAndIds(eq(nodeId), any());
    }

    @Test
    void cyclesAreCutPerBranchWithoutHidingSharedDescendants() {
        PostingInfo root = source();
        Posting a = target();
        Posting b = target();
        Posting c = target();
        link(root, a);
        link(root, b);
        link(source(a), c);
        link(source(b), c);
        link(source(c), a);

        operations.withLinkedPostings(root);

        PostingInfo firstC = root.getLinkedPostings().getFirst().getLinkedPostings().getFirst();
        PostingInfo secondC = root.getLinkedPostings().get(1).getLinkedPostings().getFirst();
        assertEquals(c.getId().toString(), firstC.getId());
        assertEquals(c.getId().toString(), secondC.getId());
        assertTrue(firstC.getLinkedPostings().isEmpty());
        assertEquals(a.getId().toString(), secondC.getLinkedPostings().getFirst().getId());
        assertTrue(secondC.getLinkedPostings().getFirst().getLinkedPostings().isEmpty());
    }

    @Test
    void invisibleIntermediatePostingsAreNotTraversed() {
        PostingInfo root = source();
        Posting visible = target();
        Posting hidden = target();
        hidden.setViewPrincipal(Principal.ADMIN);
        Posting behindHidden = target();
        link(root, visible);
        link(source(visible), hidden);
        link(source(hidden), behindHidden);
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_CONTENT)).thenReturn(false);

        operations.withLinkedPostings(root);

        assertEquals(visible.getId().toString(), root.getLinkedPostings().getFirst().getId());
        assertTrue(root.getLinkedPostings().getFirst().getLinkedPostings().isEmpty());
        verify(linkedEntryRepository, times(2)).findResolvedByRevisionIds(eq(nodeId), any());
        verify(postingRepository, times(2)).findFullByNodeIdAndIds(eq(nodeId), any());
    }

    @Test
    void longAcyclicChainIsBuiltWithoutRecursiveJavaCalls() {
        PostingInfo root = source();
        PostingInfo previous = root;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Posting target = target();
            ids.add(target.getId().toString());
            link(previous, target);
            previous = source(target);
        }

        operations.withLinkedPostings(root);

        PostingInfo current = root;
        for (String id : ids) {
            assertEquals(1, current.getLinkedPostings().size());
            current = current.getLinkedPostings().getFirst();
            assertEquals(id, current.getId());
        }
        assertTrue(current.getLinkedPostings().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void mediaChildrenWithoutParentEntryHaveNoLinkedPostings(boolean remote, boolean slice) {
        PostingInfo root = source();
        Posting media = target();
        if (remote) {
            media.setParentRemoteMedia(new RemoteMediaFile());
        } else {
            media.setParentMedia(new MediaFileOwner());
        }
        link(root, media);

        PostingInfo mediaRoot = source(media);
        if (slice) {
            operations.fillLinkedPostings(List.of(root, mediaRoot));
            assertNull(mediaRoot.getLinkedPostings());
        } else {
            operations.withLinkedPostings(root);
        }

        assertEquals(media.getId().toString(), root.getLinkedPostings().getFirst().getId());
        assertNull(root.getLinkedPostings().getFirst().getLinkedPostings());
    }

    private PostingInfo source() {
        PostingInfo source = new PostingInfo();
        source.setId(UUID.randomUUID().toString());
        source.setRevisionId(UUID.randomUUID().toString());
        return source;
    }

    private PostingInfo source(Posting posting) {
        PostingInfo source = new PostingInfo();
        source.setId(posting.getId().toString());
        source.setRevisionId(posting.getCurrentRevision().getId().toString());
        return source;
    }

    private Posting target() {
        Posting target = new Posting();
        target.setId(UUID.randomUUID());
        target.setOwnerName("author_0");
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(target);
        revision.setBody("{}");
        revision.setBodyPreview("{}");
        target.setCurrentRevision(revision);
        targets.add(target);
        return target;
    }

    private Posting remoteTarget(String nodeName) {
        Posting target = target();
        target.setReceiverName(nodeName);
        target.setReceiverEntryId("same-id");
        return target;
    }

    private void link(PostingInfo source, Posting target) {
        LinkedEntry link = new LinkedEntry();
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.fromString(source.getRevisionId()));
        link.setEntryRevision(revision);
        link.setLinkedEntry(target);
        links.add(link);
    }

    private Reaction reaction(Posting posting, Principal view, int emoji) {
        Reaction reaction = new Reaction();
        reaction.setEntryRevision(posting.getCurrentRevision());
        reaction.setViewPrincipal(view);
        reaction.setEmoji(emoji);
        return reaction;
    }

    private OwnReaction ownReaction(String nodeName, int emoji) {
        OwnReaction reaction = new OwnReaction();
        reaction.setRemoteNodeName(nodeName);
        reaction.setRemotePostingId("same-id");
        reaction.setEmoji(emoji);
        return reaction;
    }

}
