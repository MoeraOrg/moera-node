package org.moera.node.liberin.receptor;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.event.EventManager;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingRecommendationUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.EventType;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.operations.PostingOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostingRecommendationTest {

    @Test
    void clearingRecommendationEmitsPostingUpdatedEventWithVisibility() {
        Posting posting = new Posting();
        posting.setNodeId(UUID.randomUUID());
        posting.setId(UUID.randomUUID());
        posting.setViewPrincipal(Principal.ADMIN);
        posting.setRecommended(true);
        UniversalContext context = mock(UniversalContext.class);
        PostingOperations operations = new PostingOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);

        operations.unsetRecommended(posting);

        assertFalse(posting.isRecommended());
        ArgumentCaptor<PostingRecommendationUpdatedLiberin> liberins =
            ArgumentCaptor.forClass(PostingRecommendationUpdatedLiberin.class);
        verify(context).send(liberins.capture());
        PostingRecommendationUpdatedLiberin liberin = liberins.getValue();
        liberin.setNodeId(posting.getNodeId());
        liberin.setClientId("client");
        assertEquals(posting.getId(), liberin.getPostingId());
        PostingRepository repository = mock(PostingRepository.class);
        when(repository.findByNodeIdAndId(posting.getNodeId(), posting.getId())).thenReturn(Optional.of(posting));
        EventManager events = mock(EventManager.class);
        PostingReceptor receptor = new PostingReceptor();
        ReflectionTestUtils.setField(receptor, "postingRepository", repository);
        ReflectionTestUtils.setField(receptor, "eventManager", events);

        receptor.recommendationUpdated(liberin);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).send(eq(posting.getNodeId()), eq("client"), captor.capture());
        PostingUpdatedEvent event = assertInstanceOf(PostingUpdatedEvent.class, captor.getValue());
        assertEquals(EventType.POSTING_UPDATED, event.getType());
        assertEquals(posting.getId().toString(), event.getId());
        assertEquals(Principal.ADMIN, event.getFilter());
    }

    @Test
    void ordinaryPostingDoesNotEmitRecommendationUpdate() {
        UniversalContext context = mock(UniversalContext.class);
        PostingOperations operations = new PostingOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);

        operations.unsetRecommended(new Posting());

        verifyNoInteractions(context);
    }

    @Test
    void deletedPostingDoesNotEmitUpdate() {
        UUID nodeId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        PostingRepository repository = mock(PostingRepository.class);
        when(repository.findByNodeIdAndId(nodeId, postingId)).thenReturn(Optional.empty());
        EventManager events = mock(EventManager.class);
        PostingReceptor receptor = new PostingReceptor();
        ReflectionTestUtils.setField(receptor, "postingRepository", repository);
        ReflectionTestUtils.setField(receptor, "eventManager", events);
        PostingRecommendationUpdatedLiberin liberin = new PostingRecommendationUpdatedLiberin(postingId);
        liberin.setNodeId(nodeId);

        receptor.recommendationUpdated(liberin);

        verifyNoInteractions(events);
    }

}
