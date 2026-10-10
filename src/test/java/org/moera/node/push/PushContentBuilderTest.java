package org.moera.node.push;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Story;

import static org.junit.jupiter.api.Assertions.assertNull;

class PushContentBuilderTest {

    @Test
    void pushWithoutRecipientContentScopeOmitsExternalSources() {
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), StoryType.SUBSCRIBER_ADDED);
        story.setMoment(42L);
        story.setExternalSourceUri(new String[]{"https://one.example/story", "https://two.example/story"});

        PushContent content = PushContentBuilder.storyAdded(story, null);

        assertNull(content.getStory().getExternalSourceUri());
    }

}
