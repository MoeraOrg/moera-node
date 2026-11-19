package org.moera.node.liberin.receptor;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.event.EventManager;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingRecommendationUpdatedLiberin;
import org.moera.node.model.event.Event;
import org.moera.node.operations.PostingOperations;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostingRecommendationTest {

    @Test
    void clearingRecommendationSignalsTheAffectedPosting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setRecommended(true);
        UniversalContext context = mock(UniversalContext.class);
        PostingOperations operations = new PostingOperations();
        ReflectionTestUtils.setField(operations, "universalContext", context);

        operations.unsetRecommended(posting);

        assertFalse(posting.isRecommended());
        ArgumentCaptor<PostingRecommendationUpdatedLiberin> liberins =
            ArgumentCaptor.forClass(PostingRecommendationUpdatedLiberin.class);
        verify(context).send(liberins.capture());
        assertEquals(posting.getId(), liberins.getValue().getPostingId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recommendationEventCarriesCurrentFlagAndVisibility(boolean recommended) {
        Posting posting = new Posting();
        posting.setNodeId(UUID.randomUUID());
        posting.setId(UUID.randomUUID());
        posting.setViewPrincipal(Principal.ADMIN);
        posting.setRecommended(recommended);
        PostingRecommendationUpdatedLiberin liberin = new PostingRecommendationUpdatedLiberin(posting.getId());
        liberin.setNodeId(posting.getNodeId());
        liberin.setClientId("client");
        PostingRepository repository = mock(PostingRepository.class);
        when(repository.findByNodeIdAndId(posting.getNodeId(), posting.getId())).thenReturn(Optional.of(posting));
        EventManager events = mock(EventManager.class);
        PostingReceptor receptor = new PostingReceptor();
        ReflectionTestUtils.setField(receptor, "postingRepository", repository);
        ReflectionTestUtils.setField(receptor, "eventManager", events);

        receptor.recommendationUpdated(liberin);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).send(eq(posting.getNodeId()), eq("client"), captor.capture());
        Event event = captor.getValue().copy();
        assertEquals(Principal.ADMIN, event.getFilter());
        ObjectMapper mapper = new ObjectMapper();
        var payload = mapper.valueToTree(event);
        assertEquals("POSTING_RECOMMENDATION_UPDATED", payload.path("type").asString());
        assertEquals(posting.getId().toString(), payload.path("id").asString());
        assertEquals(mapper.valueToTree(recommended), payload.get("recommended"));
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
