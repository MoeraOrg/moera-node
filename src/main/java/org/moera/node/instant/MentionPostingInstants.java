package org.moera.node.instant;

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.springframework.stereotype.Component;

@Component
public class MentionPostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String ownerName, String ownerFullName, String ownerGender,
                      AvatarImage ownerAvatar, String id, String heading, List<String> sheriffs,
                      List<SheriffMark> sheriffMarks) {
        if (isBlocked(StoryType.MENTION_POSTING, null, nodeName, id, ownerName)) {
            return;
        }
        Story story = findStory(nodeName, id);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), nodeId(), StoryType.MENTION_POSTING);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(ownerName);
        story.setRemotePostingFullName(ownerFullName);
        if (ownerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(ownerAvatar));
            story.setRemotePostingAvatarShape(ownerAvatar.getShape());
        }
        story.setRemotePostingId(id);
        story.setSummaryData(buildSummary(story, ownerGender, heading, sheriffs, sheriffMarks));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    public void deleted(String nodeName, String id) {
        Story story = findStory(nodeName, id);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        storyDeleted(story);
    }

    private Story findStory(String nodeName, String id) {
        return storyRepository.findByRemotePostingId(nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING,
                nodeName, id).stream().findFirst().orElse(null);
    }

    private static StorySummaryData buildSummary(Story story, String ownerGender, String heading, List<String> sheriffs,
                                                 List<SheriffMark> sheriffMarks) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(
            story.getRemotePostingNodeName(),
            story.getRemotePostingFullName(),
            ownerGender,
            heading,
            sheriffs,
            sheriffMarks
        ));
        return summaryData;
    }

}
