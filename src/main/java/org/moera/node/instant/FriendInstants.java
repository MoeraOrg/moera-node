package org.moera.node.instant;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.FriendOf;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryFriendGroup;
import org.moera.node.model.StorySummaryNode;
import org.springframework.stereotype.Component;

@Component
public class FriendInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void added(FriendOf friend) {
        createStory(friend, StoryType.FRIEND_ADDED);
    }

    public void deleted(FriendOf friend) {
        createStory(friend, StoryType.FRIEND_DELETED);
    }

    public void groupDeleted(FriendOf friend) {
        createStory(friend, StoryType.FRIEND_GROUP_DELETED);
    }

    private void createStory(FriendOf friend, StoryType storyType) {
        if (isBlocked(storyType, null, null, null, friend.getRemoteNodeName())) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), storyType);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(friend.getRemoteNodeName());
        story.setRemoteFullName(friend.getContact().getRemoteFullName());
        story.setRemoteAvatarMediaFile(friend.getContact().getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(friend.getContact().getRemoteAvatarShape());
        story.setSummaryData(buildSummary(friend));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSummary(FriendOf friend) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(friend.getContact()));
        summaryData.setFriendGroup(
                new StorySummaryFriendGroup(friend.getRemoteGroupId(), friend.getRemoteGroupTitle()));
        return summaryData;
    }

}
