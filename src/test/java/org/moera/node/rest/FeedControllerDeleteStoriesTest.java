package org.moera.node.rest;

import java.util.UUID;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.Feed;
import org.moera.node.global.RequestContext;
import org.moera.node.operations.StoryOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedControllerDeleteStoriesTest {

    @ParameterizedTest
    @CsvSource({
        "not-a-uuid, false, false",
        "not-a-uuid, true, false",
        "not-a-uuid, false, true",
        "not-a-uuid, true, true",
        "00000000-0000-0000-0000-00000000000g, false, false",
        "' ', true, true"
    })
    void invalidPostingIdReturnsSuccessWithoutAccessingData(
        String postingid, boolean unrestricted, boolean withReceiver
    ) throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        RequestContext requestContext = mock(RequestContext.class);
        StoryOperations storyOperations = mock(StoryOperations.class);
        FeedController controller = new FeedController();
        ReflectionTestUtils.setField(controller, "entityManager", entityManager);
        ReflectionTestUtils.setField(controller, "requestContext", requestContext);
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);
        var request = delete("/moera/api/feeds/timeline/stories").param("postingid", postingid);
        if (unrestricted) {
            request.param("unrestricted", "true");
        }
        if (withReceiver) {
            request.param("receiver", "remote_0").param("receiverid", "target");
        }

        MockMvcBuilders.standaloneSetup(controller).build().perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value("ok"));

        verifyNoInteractions(entityManager, requestContext, storyOperations);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "null, null, null, null, null",
        "null, '', '', null, null",
        "null, null, null, null, false",
        "null, '', '', null, false",
        "POSTING_ADDED, null, null, null, null",
        "POSTING_ADDED, '', '', null, null",
        "POSTING_ADDED, null, null, null, false",
        "POSTING_ADDED, '', '', null, false",
        "null, null, null, '', null",
        "null, null, null, '', false",
        "POSTING_ADDED, null, null, '', null",
        "POSTING_ADDED, null, null, '', false"
    }, nullValues = "null")
    void rejectsCatastrophicDeletionBeforeAccessingStories(
        StoryType type, String receiver, String receiverid, String postingid, Boolean recommended
    ) {
        EntityManager entityManager = mock(EntityManager.class);
        StoryOperations storyOperations = mock(StoryOperations.class);
        FeedController controller = new FeedController();
        ReflectionTestUtils.setField(controller, "entityManager", entityManager);
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> controller.deleteStories(Feed.TIMELINE, type, receiver, receiverid, postingid, recommended, false)
        );

        assertEquals("feed.delete-unrestricted", failure.getErrorCode());
        verifyNoInteractions(entityManager, storyOperations);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "null, null, null, null, null, true",
        "null, null, null, '', null, true",
        "POSTING_ADDED, '', '', null, false, true",
        "null, example.org, null, null, null, false",
        "null, example.org, remote-id, '', null, false",
        "null, null, remote-id, null, null, false",
        "null, null, null, 00000000-0000-0000-0000-000000000001, null, false",
        "REACTION_ADDED_POSITIVE, null, null, null, null, false",
        "null, null, null, null, true, false",
        "POSTING_ADDED, null, null, null, true, false"
    }, nullValues = "null")
    void allowsRestrictedDeletionOrExplicitOverride(
        StoryType type, String receiver, String receiverid, String postingid, Boolean recommended, boolean unrestricted
    ) {
        StoryOperations storyOperations = mock(StoryOperations.class);
        FeedController controller = new FeedController();
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);

        assertSame(
            Result.OK,
            controller.deleteStories(Feed.TIMELINE, type, receiver, receiverid, postingid, recommended, unrestricted)
        );
        verify(storyOperations).deleteMultiple(
            Feed.TIMELINE,
            type,
            receiver,
            receiverid,
            postingid == null || postingid.isEmpty() ? null : UUID.fromString(postingid),
            recommended
        );
    }
}
