package org.moera.node.instant;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Contact;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.StorySummaryBlocked;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryNode;
import org.moera.node.util.Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class BlockedUserInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void blocked(BlockedByUser blockedByUser) {
        buildStory(StoryType.BLOCKED_USER, blockedByUser);
    }

    public void unblocked(BlockedByUser blockedByUser) {
        buildStory(StoryType.UNBLOCKED_USER, blockedByUser);
    }

    private void buildStory(StoryType storyType, BlockedByUser blockedByUser) {
        if (isBlocked(storyType, null, null, null, blockedByUser.getRemoteNodeName())) {
            return;
        }

        Contact contact = blockedByUser.getContact();

        Story story = storyRepository.findByFeed(nodeId(), Feed.INSTANT,
                PageRequest.of(0, 1, Sort.Direction.DESC, "moment")).stream()
                .findFirst()
                .orElse(null);

        boolean isNewStory = false;
        if (story == null
                || story.getStoryType() != storyType
                || !story.getRemoteNodeName().equals(contact.getRemoteNodeName())
                || story.getRemotePostingId() != null) {

            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(contact.getRemoteNodeName());
            story.setRemoteFullName(contact.getRemoteFullName());
            story.setRemoteAvatarMediaFile(contact.getRemoteAvatarMediaFile());;
            story.setRemoteAvatarShape(contact.getRemoteAvatarShape());
        }

        story.setSummaryData(buildSummary(contact, story.getSummaryData(), blockedByUser.getBlockedOperation(),
                blockedByUser.getDeadline()));
        story.setPublishedAt(Util.now());
        story.setRead(false);
        story.setViewed(false);
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAddedOrUpdated(story, isNewStory);
    }

    private static StorySummaryData buildSummary(Contact contact, StorySummaryData prevSummary,
                                                 BlockedOperation newOperation, Timestamp deadline) {
        Set<BlockedOperation> operations;
        if (prevSummary == null) {
            operations = Set.of(newOperation);
        } else {
            operations = new HashSet<>(prevSummary.getBlocked().getOperations());
            operations.add(newOperation);
        }
        Long period = deadline != null ? deadline.toInstant().getEpochSecond() - Instant.now().getEpochSecond() : null;

        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(contact.getRemoteNodeName(), contact.getRemoteFullName(),
                contact.getRemoteGender()));
        summaryData.setBlocked(new StorySummaryBlocked(operations, period));
        return summaryData;
    }

}
