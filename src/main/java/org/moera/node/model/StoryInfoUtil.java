package org.moera.node.model;

import java.util.function.Function;

import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryOperations;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class StoryInfoUtil {

    private static void buildTo(StoryInfo info, Story story, boolean isAdmin) { // See also StoryEvent constructor
        info.setId(story.getId().toString());
        info.setFeedName(story.getFeedName());
        info.setStoryType(story.getStoryType());
        info.setCreatedAt(Util.toEpochSecond(story.getCreatedAt()));
        info.setPublishedAt(Util.toEpochSecond(story.getPublishedAt()));
        info.setPinned(story.isPinned());
        info.setMoment(story.getMoment());
        if (isAdmin) {
            info.setViewed(story.isViewed());
            info.setRead(story.isRead());
            info.setSatisfied(story.isSatisfied());
        }
        if (story.getSummary().startsWith("{")) {
            info.setSummaryData(story.getSummaryData());
        } else if (story.getSummary().isEmpty()) {
            info.setSummaryData(new StorySummaryData());
        } else {
            info.setSummary(story.getSummary());
        }
        StoryOperations operations = new StoryOperations();
        operations.setEdit(Principal.ADMIN);
        operations.setDelete(Principal.ADMIN);
        info.setOperations(operations);
    }

    public static StoryInfo build(Story story, boolean isAdmin, Function<Story, PostingInfo> buildPostingInfo) {
        StoryInfo info = new StoryInfo();
        buildTo(info, story, isAdmin);

        switch (story.getStoryType()) {
            case POSTING_ADDED:
                info.setPosting(buildPostingInfo.apply(story));
                break;

            case REACTION_ADDED_POSITIVE:
            case REACTION_ADDED_NEGATIVE:
                info.setPostingId(story.getEntry().getId().toString());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                break;

            case MENTION_POSTING:
            case POSTING_SUBSCRIBE_TASK_FAILED:
            case POSTING_UPDATED:
            case POSTING_REACTION_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemotePostingAvatarMediaFile(), story.getRemotePostingAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case POSTING_UPDATE_TASK_FAILED:
            case BLOCKED_USER_IN_POSTING:
            case UNBLOCKED_USER_IN_POSTING:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteNodeName());
                info.setSummaryFullName(story.getRemoteFullName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteAvatarMediaFile(), story.getRemoteAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case SUBSCRIBER_ADDED:
            case SUBSCRIBER_DELETED:
            case POSTING_POST_TASK_FAILED:
            case FRIEND_ADDED:
            case FRIEND_DELETED:
            case FRIEND_GROUP_DELETED:
            case ASKED_TO_SUBSCRIBE:
            case ASKED_TO_FRIEND:
            case BLOCKED_USER:
            case UNBLOCKED_USER:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteNodeName());
                info.setSummaryFullName(story.getRemoteFullName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteAvatarMediaFile(), story.getRemoteAvatarShape()
                    ));
                }
                break;

            case COMMENT_ADDED:
                info.setPostingId(story.getEntry().getId().toString());
                info.setRemoteCommentId(story.getRemoteCommentId());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                break;

            case MENTION_COMMENT:
            case REPLY_COMMENT:
            case COMMENT_REACTION_ADDED_POSITIVE:
            case COMMENT_REACTION_ADDED_NEGATIVE:
            case COMMENT_REACTION_TASK_FAILED:
            case REMOTE_COMMENT_ADDED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;

            case COMMENT_UPDATE_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemotePostingAvatarMediaFile(), story.getRemotePostingAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;

            case COMMENT_POST_TASK_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemotePostingAvatarMediaFile(), story.getRemotePostingAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                break;

            case POSTING_MEDIA_REACTION_ADDED_POSITIVE:
            case POSTING_MEDIA_REACTION_ADDED_NEGATIVE:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case COMMENT_MEDIA_REACTION_ADDED_POSITIVE:
            case COMMENT_MEDIA_REACTION_ADDED_NEGATIVE:
            case COMMENT_MEDIA_REACTION_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteCommentId(story.getRemoteParentCommentId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case POSTING_MEDIA_REACTION_FAILED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setRemoteFullName(story.getRemoteFullName());
                info.setSummaryNodeName(story.getRemotePostingNodeName());
                info.setSummaryFullName(story.getRemotePostingFullName());
                if (story.getRemotePostingAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemotePostingAvatarMediaFile(), story.getRemotePostingAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemoteParentPostingId());
                info.setRemoteMediaId(story.getRemoteParentMediaId());
                break;

            case SHERIFF_MARKED:
            case SHERIFF_UNMARKED:
            case SHERIFF_COMPLAINT_DECIDED:
                info.setRemoteNodeName(story.getRemoteNodeName());
                info.setSummaryNodeName(story.getSummaryData().getSheriff().getSheriffName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteAvatarMediaFile(), story.getRemoteAvatarShape()
                    ));
                }
                info.setRemotePostingId(story.getRemotePostingId());
                info.setRemoteCommentId(story.getRemoteCommentId());
                break;

            case SHERIFF_COMPLAINT_ADDED:
                info.setSummaryNodeName(story.getSummaryData().getSheriff().getSheriffName());
                if (story.getRemoteAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteAvatarMediaFile(), story.getRemoteAvatarShape()
                    ));
                }
                break;

            case DEFROSTING:
                info.setSummaryNodeName(story.getRemoteOwnerName());
                info.setSummaryFullName(story.getRemoteOwnerFullName());
                if (story.getRemoteOwnerAvatarMediaFile() != null) {
                    info.setSummaryAvatar(AvatarImageUtil.build(
                        story.getRemoteOwnerAvatarMediaFile(), story.getRemoteOwnerAvatarShape()
                    ));
                }
                break;
        }

        return info;
    }

}
