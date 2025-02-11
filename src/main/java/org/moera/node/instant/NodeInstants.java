package org.moera.node.instant;

import java.util.UUID;

import jakarta.inject.Inject;

import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class NodeInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void defrosting() {
        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.DEFROSTING);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteOwnerName(nodeName());
        story.setRemoteOwnerFullName(universalContext.fullName());
        if (universalContext.getAvatar() != null) {
            story.setRemoteOwnerAvatarMediaFile(universalContext.getAvatar().getMediaFile());
            story.setRemoteOwnerAvatarShape(universalContext.getAvatar().getShape());
        }
        story.setRead(false);
        story.setViewed(false);
        story.setPublishedAt(Util.now());
        story.setMoment(0L);
        story = storyRepository.save(story);
        updateMoment(story);
        storyAddedOrUpdated(story, true);
    }

}
