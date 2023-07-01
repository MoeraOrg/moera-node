package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.node.data.SheriffMark;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class ForeignCommentAddedLiberin extends Liberin {

    private String nodeName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingOwnerGender;
    private AvatarImage postingOwnerAvatar;
    private String postingId;
    private String postingHeading;
    private List<String> postingSheriffs;
    private List<SheriffMark> postingSheriffMarks;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private String commentOwnerGender;
    private AvatarImage commentOwnerAvatar;
    private String commentId;
    private String commentHeading;
    private List<SheriffMark> commentSheriffMarks;
    private SubscriptionReason subscriptionReason;

    public ForeignCommentAddedLiberin(String nodeName, String postingOwnerName, String postingOwnerFullName,
                                      String postingOwnerGender, AvatarImage postingOwnerAvatar, String postingId,
                                      String postingHeading, List<String> postingSheriffs,
                                      List<SheriffMark> postingSheriffMarks, String commentOwnerName,
                                      String commentOwnerFullName, String commentOwnerGender,
                                      AvatarImage commentOwnerAvatar, String commentId, String commentHeading,
                                      List<SheriffMark> commentSheriffMarks, SubscriptionReason subscriptionReason) {
        this.nodeName = nodeName;
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerGender = postingOwnerGender;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingId = postingId;
        this.postingHeading = postingHeading;
        this.postingSheriffs = postingSheriffs;
        this.postingSheriffMarks = postingSheriffMarks;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentOwnerGender = commentOwnerGender;
        this.commentOwnerAvatar = commentOwnerAvatar;
        this.commentId = commentId;
        this.commentHeading = commentHeading;
        this.commentSheriffMarks = commentSheriffMarks;
        this.subscriptionReason = subscriptionReason;
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

    public String getCommentOwnerGender() {
        return commentOwnerGender;
    }

    public void setCommentOwnerGender(String commentOwnerGender) {
        this.commentOwnerGender = commentOwnerGender;
    }

    public AvatarImage getCommentOwnerAvatar() {
        return commentOwnerAvatar;
    }

    public void setCommentOwnerAvatar(AvatarImage commentOwnerAvatar) {
        this.commentOwnerAvatar = commentOwnerAvatar;
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

    public List<SheriffMark> getCommentSheriffMarks() {
        return commentSheriffMarks;
    }

    public void setCommentSheriffMarks(List<SheriffMark> commentSheriffMarks) {
        this.commentSheriffMarks = commentSheriffMarks;
    }

    public SubscriptionReason getSubscriptionReason() {
        return subscriptionReason;
    }

    public void setSubscriptionReason(SubscriptionReason subscriptionReason) {
        this.subscriptionReason = subscriptionReason;
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
        model.put("commentOwnerName", commentOwnerName);
        model.put("commentOwnerFullName", commentOwnerFullName);
        model.put("commentOwnerGender", commentOwnerGender);
        model.put("commentOwnerAvatar", commentOwnerAvatar);
        model.put("commentId", commentId);
        model.put("commentHeading", commentHeading);
        model.put("commentSheriffMarks", commentSheriffMarks);
        model.put("subscriptionReason", subscriptionReason);
    }

}
