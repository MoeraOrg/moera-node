package org.moera.node.model;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoryAttributesUtilTest {

    @Test
    void nullExternalSourceIsRejectedBeforeApplyingAttributes() {
        Story story = new Story();
        story.setFeedName(Feed.NEWS);
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(Feed.TIMELINE);
        attributes.setExternalSourceUri(Arrays.asList("https://valid.example/story", null));

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> StoryAttributesUtil.toStory(attributes, story)
        );

        assertEquals("story.external-source-uri.missing", failure.getErrorCode());
        assertEquals(Feed.NEWS, story.getFeedName());
    }

}
