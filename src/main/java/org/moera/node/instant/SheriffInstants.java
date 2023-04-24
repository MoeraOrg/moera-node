package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.model.StorySummarySheriff;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class SheriffInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void orderForFeed(String remoteFeedName, String sheriffName, String orderId) {
        buildStory(StoryType.FEED_SHERIFF_MARKED, nodeName(), remoteFeedName, null, null, null, null, null, null,
                sheriffName, orderId);
    }

    public void orderForPosting(String remoteNodeName, String remoteFeedName, String postingHeading, String postingId,
                                String sheriffName, String orderId) {
        buildStory(StoryType.POSTING_SHERIFF_MARKED, remoteNodeName, remoteFeedName, null, null, postingHeading,
                postingId, null, null, sheriffName, orderId);
    }

    public void orderForComment(String remoteNodeName, String remoteFeedName, String postingOwnerName,
                                String postingOwnerFullName, String postingHeading, String postingId,
                                String commentHeading, String commentId, String sheriffName, String orderId) {
        buildStory(StoryType.COMMENT_SHERIFF_MARKED, remoteNodeName, remoteFeedName, postingOwnerName,
                postingOwnerFullName, postingHeading, postingId, commentHeading, commentId, sheriffName, orderId);
    }

    public void deletedOrderForFeed(String remoteFeedName, String sheriffName, String orderId) {
        buildStory(StoryType.FEED_SHERIFF_UNMARKED, null, remoteFeedName, null, null, null, null, null, null,
                sheriffName, orderId);
    }

    public void deletedOrderForPosting(String remoteNodeName, String remoteFeedName, String postingHeading,
                                       String postingId, String sheriffName, String orderId) {
        buildStory(StoryType.POSTING_SHERIFF_UNMARKED, remoteNodeName, remoteFeedName, null, null, postingHeading,
                postingId, null, null, sheriffName, orderId);
    }

    public void deletedOrderForComment(String remoteNodeName, String remoteFeedName, String postingOwnerName,
                                       String postingOwnerFullName, String postingHeading, String postingId,
                                       String commentHeading, String commentId, String sheriffName, String orderId) {
        buildStory(StoryType.COMMENT_SHERIFF_UNMARKED, remoteNodeName, remoteFeedName, postingOwnerName,
                postingOwnerFullName, postingHeading, postingId, commentHeading, commentId, sheriffName, orderId);
    }

    private void buildStory(StoryType storyType, String remoteNodeName, String remoteFeedName, String postingOwnerName,
                            String postingOwnerFullName, String postingHeading, String postingId, String commentHeading,
                            String commentId, String sheriffName, String orderId) {
        if (isBlocked(storyType)) {
            return;
        }
        Story story = new Story(UUID.randomUUID(), nodeId(), storyType);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingId(postingId);
        story.setRemoteCommentId(commentId);
        story.setSummaryData(buildSummary(remoteFeedName, postingOwnerName, postingOwnerFullName, postingHeading,
                commentHeading, sheriffName, orderId));
        story.setPublishedAt(Util.now());
        story.setRead(false);
        story.setViewed(false);
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSummary(String remoteFeedName, String postingOwnerName,
                                                 String postingOwnerFullName, String postingHeading,
                                                 String commentHeading, String sheriffName, String orderId) {
        StorySummaryData summaryData = new StorySummaryData();
        if (postingHeading != null) {
            summaryData.setPosting(new StorySummaryEntry(postingOwnerName, postingOwnerFullName, null, postingHeading));
        }
        if (commentHeading != null) {
            summaryData.setComment(new StorySummaryEntry(null, null, null, commentHeading));
        }
        summaryData.setFeedName(remoteFeedName);
        summaryData.setSheriff(new StorySummarySheriff(sheriffName, orderId));
        return summaryData;
    }

}
