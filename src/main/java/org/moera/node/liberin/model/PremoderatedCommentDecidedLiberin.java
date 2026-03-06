package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.PremoderatedCommentDecidedNotification;
import org.moera.node.liberin.Liberin;

public class PremoderatedCommentDecidedLiberin extends Liberin {

    private String nodeName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingOwnerGender;
    private AvatarImage postingOwnerAvatar;
    private String postingId;
    private String postingHeading;
    private List<String> postingSheriffs;
    private List<SheriffMark> postingSheriffMarks;
    private String commentId;
    private String commentHeading;
    private boolean accepted;

    public PremoderatedCommentDecidedLiberin(PremoderatedCommentDecidedNotification notification) {
        nodeName = notification.getSenderNodeName();
        postingOwnerName = notification.getPostingOwnerName();
        postingOwnerFullName = notification.getPostingOwnerFullName();
        postingOwnerGender = notification.getPostingOwnerGender();
        postingOwnerAvatar = notification.getPostingOwnerAvatar();
        postingId = notification.getPostingId();
        postingHeading = notification.getPostingHeading();
        postingSheriffs = notification.getPostingSheriffs();
        postingSheriffMarks = notification.getPostingSheriffMarks();
        commentId = notification.getCommentId();
        commentHeading = notification.getCommentHeading();
        accepted = notification.isAccepted();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
    }

    public AvatarImage getPostingOwnerAvatar() {
        return postingOwnerAvatar;
    }

    public void setPostingOwnerAvatar(AvatarImage postingOwnerAvatar) {
        this.postingOwnerAvatar = postingOwnerAvatar;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public List<String> getPostingSheriffs() {
        return postingSheriffs;
    }

    public void setPostingSheriffs(List<String> postingSheriffs) {
        this.postingSheriffs = postingSheriffs;
    }

    public List<SheriffMark> getPostingSheriffMarks() {
        return postingSheriffMarks;
    }

    public void setPostingSheriffMarks(List<SheriffMark> postingSheriffMarks) {
        this.postingSheriffMarks = postingSheriffMarks;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingOwnerName", postingOwnerName);
        model.put("postingOwnerFullName", postingOwnerFullName);
        model.put("postingOwnerGender", postingOwnerGender);
        model.put("postingOwnerAvatar", postingOwnerAvatar);
        model.put("postingId", postingId);
        model.put("postingHeading", postingHeading);
        model.put("postingSheriffs", postingSheriffs);
        model.put("postingSheriffMarks", postingSheriffMarks);
        model.put("commentId", commentId);
        model.put("commentHeading", commentHeading);
        model.put("accepted", accepted);
    }

}
