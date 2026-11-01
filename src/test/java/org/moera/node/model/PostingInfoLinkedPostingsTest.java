package org.moera.node.model;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.PostingInfoOperations;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.util.Util;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostingInfoLinkedPostingsTest {

    @Test
    void onlyVisibleLiveResolvedTargetsAreIncluded() {
        Posting posting = posting();
        Posting visible = posting();
        Posting privatePosting = posting();
        privatePosting.setViewPrincipal(Principal.ADMIN);
        Posting deleted = posting();
        deleted.setDeletedAt(Util.now());
        link(posting, visible);
        link(posting, privatePosting);
        link(posting, deleted);
        link(posting, null);

        PostingInfo info = info(posting, AccessCheckers.PUBLIC, visible, privatePosting, deleted);

        assertEquals(List.of(visible.getId().toString()), info.getLinkedPostings().stream()
            .map(PostingInfo::getId).toList());
        assertEquals(2, info(posting, AccessCheckers.ADMIN, visible, privatePosting, deleted).getLinkedPostings().size());
    }

    @Test
    void viewContentScopeIsRequiredForLinkedPostings() {
        Posting posting = posting();
        Posting target = posting();
        link(posting, target);
        AccessChecker checker = (principal, scope) -> scope != Scope.VIEW_CONTENT;

        assertTrue(info(posting, checker, target).getLinkedPostings().isEmpty());
    }

    @Test
    void repeatedTargetsAreIncludedOnceAndCyclesAreNotExpanded() {
        Posting posting = posting();
        Posting target = posting();
        link(posting, target);
        link(posting, target);
        link(target, posting);
        link(target, target);

        PostingInfo info = info(posting, AccessCheckers.PUBLIC, target);

        assertEquals(1, info.getLinkedPostings().size());
        assertEquals(target.getId().toString(), info.getLinkedPostings().getFirst().getId());
        assertTrue(info.getLinkedPostings().getFirst().getLinkedPostings().isEmpty());
    }

    @Test
    void mediaPostingDoesNotExposeLinkedPostings() {
        Posting posting = posting();
        posting.setParentMediaEntry(posting());
        MediaFileOwner media = new MediaFileOwner();
        media.setId(UUID.randomUUID());
        posting.setParentMedia(media);

        assertTrue(info(posting, AccessCheckers.PUBLIC).getLinkedPostings().isEmpty());
        posting.setParentMediaEntry(null);
        assertTrue(info(posting, AccessCheckers.PUBLIC).getLinkedPostings().isEmpty());
    }

    private static PostingInfo info(Posting posting, AccessChecker checker, Posting... targets) {
        RequestContext context = mock(RequestContext.class);
        when(context.isPrincipal(any(), any())).thenAnswer(invocation ->
            checker.isPrincipal(invocation.getArgument(0), invocation.getArgument(1))
        );
        LinkedEntryRepository linkedEntryRepository = mock(LinkedEntryRepository.class);
        var graph = Stream.concat(Stream.of(posting), Stream.of(targets)).toList();
        when(linkedEntryRepository.findResolvedByRevisionIds(any(), any())).thenAnswer(invocation -> {
            Collection<UUID> revisionIds = invocation.getArgument(1);
            return graph.stream().map(Posting::getCurrentRevision)
                .filter(revision -> revisionIds.contains(revision.getId()))
                .flatMap(revision -> revision.getLinkedEntries().stream())
                .filter(link -> link.getLinkedEntry() != null).toList();
        });
        PostingRepository postingRepository = mock(PostingRepository.class);
        when(postingRepository.findFullByNodeIdAndIds(any(), any())).thenAnswer(invocation -> {
            Collection<UUID> postingIds = invocation.getArgument(1);
            return graph.stream().filter(target -> postingIds.contains(target.getId()))
                .filter(target -> target.getDeletedAt() == null).toList();
        });
        EntryOperations entryOperations = mock(EntryOperations.class);
        when(entryOperations.prepareMediaAttachments(any())).thenReturn(MediaAttachmentsProvider.NONE);
        PostingInfoOperations operations = new PostingInfoOperations();
        ReflectionTestUtils.setField(operations, "requestContext", context);
        ReflectionTestUtils.setField(operations, "linkedEntryRepository", linkedEntryRepository);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "storyRepository", mock(StoryRepository.class));
        ReflectionTestUtils.setField(operations, "entryOperations", entryOperations);
        ReflectionTestUtils.setField(operations, "sheriffUserListOperations", mock(SheriffUserListOperations.class));
        return operations.withLinkedPostings(PostingInfoUtil.build(
            posting, MediaAttachmentsProvider.NONE, checker, null, mock(DirectServeOperations.class)
        ));
    }

    private static Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setOwnerName("owner_0");
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBody("{\"text\":\"posting\"}");
        revision.setBodyPreview("{\"text\":\"posting\"}");
        posting.setCurrentRevision(revision);
        return posting;
    }

    private static void link(Posting posting, Posting target) {
        LinkedEntry link = new LinkedEntry();
        link.setId(UUID.randomUUID());
        link.setLinkedEntry(target);
        posting.getCurrentRevision().addLinkedEntry(link);
    }

}
