package org.moera.node.rest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.StoryType;
import org.moera.node.model.converter.StringToStoryTypeConverter;
import org.moera.node.operations.StoryOperations;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoryControllerDeleteStoriesTest {

    private final StoryOperations storyOperations = mock(StoryOperations.class);

    private MockMvc mvc() {
        StoryController controller = new StoryController();
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        ExceptionsControllerAdvice advice = new ExceptionsControllerAdvice();
        ReflectionTestUtils.setField(advice, "messageSource", messageSource);
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToStoryTypeConverter());
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(advice)
            .setConversionService(conversionService)
            .build();
    }

    @ParameterizedTest
    @CsvSource({"not-a-uuid, false", "not-a-uuid, true", "' ', true"})
    void invalidPostingIdDoesNotDeleteAnything(String postingId, boolean unrestricted) throws Exception {
        mvc().perform(delete("/moera/api/stories")
                .param("postingid", postingId)
                .param("unrestricted", Boolean.toString(unrestricted)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value("ok"));

        verifyNoInteractions(storyOperations);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "null, null, null, null, null",
        "null, '', '', '', null",
        "null, null, null, null, false",
        "posting-added, null, null, null, null",
        "posting-added, '', '', '', false"
    }, nullValues = "null")
    void catastrophicDeletionRequiresExplicitPermission(
        String type, String receiver, String receiverId, String postingId, String recommended
    ) throws Exception {
        var request = delete("/moera/api/stories");
        if (type != null) {
            request.param("type", type);
        }
        if (receiver != null) {
            request.param("receiver", receiver);
        }
        if (receiverId != null) {
            request.param("receiverid", receiverId);
        }
        if (postingId != null) {
            request.param("postingid", postingId);
        }
        if (recommended != null) {
            request.param("recommended", recommended);
        }

        mvc().perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("story.delete-unrestricted"));

        verifyNoInteractions(storyOperations);
    }

    @Test
    void acceptsAllFilters() throws Exception {
        mvc().perform(delete("/moera/api/stories")
                .param("type", "posting-added")
                .param("receiver", "remote_0")
                .param("receiverid", "remote-post")
                .param("postingid", "00000000-0000-0000-0000-000000000001")
                .param("recommended", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value("ok"));
        verify(storyOperations).deleteMultiple(
            null,
            StoryType.POSTING_ADDED,
            "remote_0",
            "remote-post",
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            true
        );
    }

    @Test
    void unrestrictedAllowsDeletingWithoutFilters() throws Exception {
        mvc().perform(delete("/moera/api/stories").param("unrestricted", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value("ok"));
        verify(storyOperations).deleteMultiple(null, null, null, null, null, null);
    }

}
