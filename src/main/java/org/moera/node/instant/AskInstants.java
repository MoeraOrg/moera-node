package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryFriendGroup;
import org.moera.node.model.StorySummaryNode;
import org.springframework.stereotype.Component;

@Component
public class AskInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void askedToSubscribe(String remoteNodeName, String remoteFullName, String remoteGender,
                                 AvatarImage remoteAvatar, String message) {
        if (isBlocked(StoryType.ASKED_TO_SUBSCRIBE, null, null, null, remoteNodeName)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.ASKED_TO_SUBSCRIBE);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
        story.setRemoteAvatarShape(remoteAvatar.getShape());
        story.setSummaryData(buildSubscribeSummary(remoteNodeName, remoteFullName, remoteGender, message));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSubscribeSummary(String remoteNodeName, String remoteFullName,
                                                          String remoteGender, String message) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(remoteNodeName, remoteFullName, remoteGender));
        summaryData.setDescription(message);
        return summaryData;
    }

    public void askedToFriend(String remoteNodeName, String remoteFullName, String remoteGender,
                              AvatarImage remoteAvatar, UUID friendGroupId, String friendGroupTitle, String message) {
        if (isBlocked(StoryType.ASKED_TO_FRIEND, null, null, null, remoteNodeName)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.ASKED_TO_FRIEND);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
        story.setRemoteAvatarShape(remoteAvatar.getShape());
        story.setSummaryData(buildFriendSummary(remoteNodeName, remoteFullName, remoteGender, friendGroupId,
                friendGroupTitle, message));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildFriendSummary(String remoteNodeName, String remoteFullName,
                                                       String remoteGender, UUID friendGroupId, String friendGroupTitle,
                                                       String message) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(remoteNodeName, remoteFullName, remoteGender));
        summaryData.setFriendGroup(new StorySummaryFriendGroup(friendGroupId.toString(), friendGroupTitle));
        summaryData.setDescription(message);
        return summaryData;
    }

}
