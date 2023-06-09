package org.moera.node.model.notification;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class SheriffComplainDecidedNotification extends Notification {

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

    @NotBlank
    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 63)
    private String postingOwnerName;

    @Size(max = 96)
    private String postingOwnerFullName;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 40)
    private String postingId;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    @Size(max = 255)
    private String commentHeading;

    @Size(max = 40)
    private String commentId;

    @NotBlank
    @Size(max = 40)
    private String complainGroupId;

    public SheriffComplainDecidedNotification() {
        super(NotificationType.SHERIFF_COMPLAIN_DECIDED);
    }

    public SheriffComplainDecidedNotification(String remoteNodeName, String remoteFeedName, String postingOwnerName,
                                              String postingOwnerFullName, String postingHeading, String postingId,
                                              String commentOwnerName, String commentOwnerFullName,
                                              String commentHeading, String commentId, String complainGroupId) {
        super(NotificationType.SHERIFF_COMPLAIN_DECIDED);
        this.remoteNodeName = remoteNodeName;
        this.remoteFeedName = remoteFeedName;
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingHeading = postingHeading;
        this.postingId = postingId;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentHeading = commentHeading;
        this.commentId = commentId;
        this.complainGroupId = complainGroupId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getComplainGroupId() {
        return complainGroupId;
    }

    public void setComplainGroupId(String complainGroupId) {
        this.complainGroupId = complainGroupId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remoteFeedName", LogUtil.format(remoteFeedName)));
        parameters.add(Pair.of("postingOwnerName", LogUtil.format(postingOwnerName)));
        parameters.add(Pair.of("postingOwnerFullName", LogUtil.format(postingOwnerFullName)));
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("commentOwnerName", LogUtil.format(commentOwnerName)));
        parameters.add(Pair.of("commentOwnerFullName", LogUtil.format(commentOwnerFullName)));
        parameters.add(Pair.of("commentHeading", LogUtil.format(commentHeading)));
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
        parameters.add(Pair.of("complainGroupId", LogUtil.format(complainGroupId)));
    }

}
