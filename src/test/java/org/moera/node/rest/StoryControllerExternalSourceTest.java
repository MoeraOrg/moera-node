package org.moera.node.rest;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoryControllerExternalSourceTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private StoryController storyController;

    @Test
    void getByExternalSourceReturnsAllMatchingStoriesWithProtectedMetadata() throws Exception {
        UUID nodeId = UUID.randomUUID();
        String external = "https://source.example/story?item=1&lang=ru";
        Story first = new Story(UUID.randomUUID(), nodeId, StoryType.SUBSCRIBER_ADDED);
        first.setFeedName(Feed.INSTANT);
        first.setMoment(42L);
        first.setRemoteNodeName("alice");
        first.setViewed(true);
        first.setExternalSourceUri(new String[]{external, "https://other.example/story"});
        Story second = new Story(UUID.randomUUID(), nodeId, StoryType.FRIEND_ADDED);
        second.setFeedName(Feed.INSTANT);
        second.setMoment(41L);
        second.setRemoteNodeName("bob");
        second.setExternalSourceUri(new String[]{external});
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_FEEDS)).thenReturn(false);
        when(requestContext.isPrincipal(Principal.ADMIN, Scope.VIEW_CONTENT)).thenReturn(true);
        when(storyRepository.findByExternalSourceUri(nodeId, external)).thenReturn(List.of(first, second));

        MockMvcBuilders.standaloneSetup(storyController).build()
            .perform(get("/moera/api/stories").param("external", external))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(first.getId().toString()))
            .andExpect(jsonPath("$[0].remoteNodeName").value("alice"))
            .andExpect(jsonPath("$[0].externalSourceUri[0]").value(external))
            .andExpect(jsonPath("$[0].externalSourceUri[1]").value("https://other.example/story"))
            .andExpect(jsonPath("$[0].viewed").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(second.getId().toString()))
            .andExpect(jsonPath("$[1].remoteNodeName").value("bob"));
    }

    @Test
    void getByExternalSourceReturnsAnEmptyArrayWhenNoStoriesMatch() throws Exception {
        UUID nodeId = UUID.randomUUID();
        String external = "https://source.example/missing";
        when(requestContext.nodeId()).thenReturn(nodeId);
        when(storyRepository.findByExternalSourceUri(nodeId, external)).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(storyController).build()
            .perform(get("/moera/api/stories").param("external", external))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

}
