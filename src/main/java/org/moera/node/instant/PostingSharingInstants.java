package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.model.StorySummaryNodeUtil;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingSharingInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.ofHours(6);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private DirectServeOperations directServeOperations;

    public void added(Posting posting, EntrySharing sharing) {
        if (
            Objects.equals(sharing.getRemoteNodeName(), nodeName())
            || isBlocked(StoryType.POSTING_SHARED, posting.getId(), null, null, sharing.getRemoteNodeName())
        ) {
            return;
        }

        List<Story> groups = storyRepository.findFullByFeedAndTypeAndEntryId(
            nodeId(), Feed.INSTANT, StoryType.POSTING_SHARED, posting.getId()
        );
        if (
            groups.stream()
                .flatMap(group -> group.getSubstories().stream())
                .anyMatch(substory ->
                    substory.getSharing() != null && Objects.equals(substory.getSharing().getId(), sharing.getId())
                )
        ) {
            return;
        }

        Story story = groups.stream().findFirst().orElse(null);
        boolean isNew = story == null
            || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now());
        if (isNew) {
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_SHARED);
            story.setFeedName(Feed.INSTANT);
            story.setEntry(posting);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }
        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_SHARED);
        substory.setEntry(posting);
        substory.setSharing(sharing);
        substory.setRemoteOwnerName(sharing.getRemoteNodeName());
        substory.setRemoteOwnerFullName(sharing.getRemoteFullName());
        substory.setRemoteOwnerSourceUri(sharing.getRemoteSourceUri());
        substory.setRemoteOwnerAvatarMediaFile(sharing.getRemoteAvatarMediaFile());
        substory.setRemoteOwnerAvatarShape(sharing.getRemoteAvatarShape());
        substory.setSummaryData(buildAddedSummary(sharing));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);
        updated(story, isNew, true);
    }

    private static StorySummaryData buildAddedSummary(EntrySharing sharing) {
        StorySummaryData summary = new StorySummaryData();
        summary.setNode(
            StorySummaryNodeUtil.build(
                sharing.getRemoteNodeName(),
                sharing.getRemoteFullName(),
                sharing.getRemoteSourceUri(),
                sharing.getRemoteGender()
            )
        );
        return summary;
    }

    public void deleted(UUID postingId, Collection<UUID> substoryIds) {
        if (substoryIds.isEmpty()) {
            return;
        }
        List<Story> groups = storyRepository.findFullByFeedAndTypeAndEntryId(
            nodeId(), Feed.INSTANT, StoryType.POSTING_SHARED, postingId
        );
        for (Story story : groups) {
            List<Story> removed = story.getSubstories().stream()
                .filter(substory -> substoryIds.contains(substory.getId()))
                .toList();
            if (!removed.isEmpty()) {
                for (Story substory : removed) {
                    story.removeSubstory(substory);
                    storyRepository.delete(substory);
                }
                updated(story, false, false);
            }
        }
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> substories = story.getSubstories().stream()
            .sorted(Comparator.comparing(Story::getCreatedAt).thenComparing(Story::getId).reversed())
            .toList();
        if (substories.isEmpty()) {
            storyRepository.delete(story);
            storyDeleted(story);
            return;
        }
        StorySummaryData summary = new StorySummaryData();
        summary.setSharings(
            substories.stream().limit(2).map(Story::getSummaryData).map(StorySummaryData::getNode).toList()
        );
        summary.setTotalSharings(substories.size());
        PostingInfo posting = PostingInfoUtil.build(
            story.getEntry(),
            story.getEntry().getStories(),
            MediaAttachmentsProvider.NONE,
            AccessCheckers.ADMIN,
            universalContext.getOptions(),
            directServeOperations
        );
        summary.setPosting(
            StorySummaryEntryUtil.build(
                posting.getOwnerName(),
                posting.getOwnerFullName(),
                posting.getOwnerSourceUri(),
                posting.getOwnerGender(),
                posting.getHeading(),
                posting.getSheriffs(),
                posting.getSheriffMarks()
            )
        );
        story.setSummaryData(summary);
        Story latest = substories.getFirst();
        story.setSharing(latest.getSharing());
        story.setRemoteOwnerName(latest.getRemoteOwnerName());
        story.setRemoteOwnerFullName(latest.getRemoteOwnerFullName());
        story.setRemoteOwnerSourceUri(latest.getRemoteOwnerSourceUri());
        story.setRemoteOwnerAvatarMediaFile(latest.getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(latest.getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

}
