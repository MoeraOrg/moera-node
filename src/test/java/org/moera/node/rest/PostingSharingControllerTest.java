package org.moera.node.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SharingInfo;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.EntrySharingRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.SharingOperations;
import org.moera.node.util.SafeInteger;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class PostingSharingControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private EntrySharingRepository entrySharingRepository;

    @Mock
    private DirectServeOperations directServeOperations;

    @Spy
    private SharingOperations sharingOperations = new SharingOperations();

    @InjectMocks
    private PostingSharingController postingSharingController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sharingOperations, "entrySharingRepository", entrySharingRepository);
        ReflectionTestUtils.setField(sharingOperations, "directServeOperations", directServeOperations);
    }

    @Test
    void returnsASliceInsteadOfAnUnboundedList() throws Exception {
        Posting posting = visiblePosting();
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE,
            SafeInteger.MAX_VALUE, PageRequest.of(0, 2))).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(postingSharingController).build()
            .perform(get("/moera/api/postings/{postingId}/sharings", posting.getId()).param("limit", "1"))
            .andExpect(jsonPath("$.sharings").isArray())
            .andExpect(jsonPath("$.before").value(SafeInteger.MAX_VALUE))
            .andExpect(jsonPath("$.after").value(SafeInteger.MIN_VALUE))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void invisiblePostingIsNotFound() {
        Posting posting = posting();
        assertThrows(ObjectNotFoundFailure.class,
            () -> postingSharingController.get(posting.getId().toString(), null, null));
        verifyNoInteractions(entrySharingRepository);
    }

    @Test
    void hiddenListIsNotReadEvenWhenThePostingIsVisible() {
        Posting posting = visiblePosting();
        posting.setViewSharingsPrincipal(Principal.PRIVATE);
        var slice = postingSharingController.get(posting.getId().toString(), null, null);
        assertEquals(List.of(), slice.getSharings());
        assertEquals(0, slice.getTotal());
        verifyNoInteractions(entrySharingRepository);
    }

    @Test
    void visibleListDoesNotRevealAHiddenCounter() {
        Posting posting = visiblePosting();
        posting.setViewTotalSharingsPrincipal(Principal.PRIVATE);
        posting.setTotalSharings(10);
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE,
            SafeInteger.MAX_VALUE, PageRequest.of(0, 201))).thenReturn(List.of(sharing(posting, "alice", 30)));

        var slice = postingSharingController.get(posting.getId().toString(), null, null);

        assertEquals(1, slice.getSharings().size());
        assertEquals("alice", slice.getSharings().getFirst().getRemoteNodeName());
        assertEquals("copy", slice.getSharings().getFirst().getRemotePostingId());
        assertEquals(posting.getId().toString(), slice.getSharings().getFirst().getPostingId());
        assertEquals(30, slice.getSharings().getFirst().getMoment());
        assertEquals(0, slice.getTotal());
    }

    @Test
    void extraRowIsOnlyUsedAsTheNextInclusiveBoundary() {
        Posting posting = visiblePosting();
        posting.setTotalSharings(3);
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE, 40,
            PageRequest.of(0, 3))).thenReturn(List.of(
                sharing(posting, "alice", 30), sharing(posting, "bob", 20), sharing(posting, "carol", 10)
            ));

        var slice = postingSharingController.get(posting.getId().toString(), 40L, 2);

        assertEquals(List.of("alice", "bob"), slice.getSharings().stream().map(SharingInfo::getRemoteNodeName).toList());
        assertEquals(40, slice.getBefore());
        assertEquals(10, slice.getAfter());
        assertEquals(3, slice.getTotal());
    }

    @Test
    void pickedCounterRespectsTheOriginalPostingPermission() {
        Posting posting = visiblePosting();
        posting.setReceiverName("original");
        posting.setReceiverViewTotalSharingsPrincipal(Principal.PRIVATE);
        posting.setTotalSharings(10);
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE,
            SafeInteger.MAX_VALUE, PageRequest.of(0, 201))).thenReturn(List.of());
        assertEquals(0, postingSharingController.get(posting.getId().toString(), null, null).getTotal());
    }

    @Test
    void oversizedLimitIsCapped() {
        Posting posting = visiblePosting();
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE,
            SafeInteger.MAX_VALUE, PageRequest.of(0, 201))).thenReturn(List.of());
        assertEquals(List.of(), postingSharingController.get(posting.getId().toString(), null, 1000).getSharings());
    }

    @Test
    void negativeLimitIsRejected() {
        Posting posting = visiblePosting();
        ValidationFailure failure = assertThrows(ValidationFailure.class,
            () -> postingSharingController.get(posting.getId().toString(), null, -1));
        assertEquals("limit.invalid", failure.getErrorCode());
        verifyNoInteractions(entrySharingRepository);
    }

    @Test
    void zeroLimitReturnsNoRowsAndKeepsTheNextBoundary() {
        Posting posting = visiblePosting();
        when(entrySharingRepository.findSlice(posting.getId(), SafeInteger.MIN_VALUE, 40,
            PageRequest.of(0, 1))).thenReturn(List.of(sharing(posting, "alice", 30)));
        var slice = postingSharingController.get(posting.getId().toString(), 40L, 0);
        assertEquals(List.of(), slice.getSharings());
        assertEquals(30, slice.getAfter());
    }

    private EntrySharing sharing(Posting posting, String nodeName, long moment) {
        EntrySharing sharing = new EntrySharing();
        sharing.setId(UUID.randomUUID());
        sharing.setEntry(posting);
        sharing.setSharingType(SharingType.REPUBLISH);
        sharing.setRemoteNodeName(nodeName);
        sharing.setRemotePostingId("copy");
        sharing.setMoment(moment);
        return sharing;
    }

    private Posting visiblePosting() {
        Posting posting = posting();
        when(requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)).thenReturn(true);
        return posting;
    }

    private Posting posting() {
        UUID nodeId = UUID.randomUUID();
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(postingRepository.findByNodeIdAndId(nodeId, posting.getId())).thenReturn(Optional.of(posting));
        return posting;
    }

}
