package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class RemoteCommentReactionAddedLiberin extends Liberin {

    private String nodeName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private AvatarImage postingOwnerAvatar;
    private String postingId;
    private String commentId;
    private String reactionNodeName;
    private String reactionFullName;
    private AvatarImage reactionAvatar;
    private String commentHeading;
    private boolean reactionNegative;
    private int reactionEmoji;

    public RemoteCommentReactionAddedLiberin(String nodeName, String postingOwnerName, String postingOwnerFullName,
                                             AvatarImage postingOwnerAvatar, String postingId, String commentId,
                                             String reactionNodeName, String reactionFullName,
                                             AvatarImage reactionAvatar, String commentHeading,
                                             boolean reactionNegative, int reactionEmoji) {
        this.nodeName = nodeName;
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingId = postingId;
        this.commentId = commentId;
        this.reactionNodeName = reactionNodeName;
        this.reactionFullName = reactionFullName;
        this.reactionAvatar = reactionAvatar;
        this.commentHeading = commentHeading;
        this.reactionNegative = reactionNegative;
        this.reactionEmoji = reactionEmoji;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getReactionNodeName() {
        return reactionNodeName;
    }

    public void setReactionNodeName(String reactionNodeName) {
        this.reactionNodeName = reactionNodeName;
    }

    public String getReactionFullName() {
        return reactionFullName;
    }

    public void setReactionFullName(String reactionFullName) {
        this.reactionFullName = reactionFullName;
    }

    public AvatarImage getReactionAvatar() {
        return reactionAvatar;
    }

    public void setReactionAvatar(AvatarImage reactionAvatar) {
        this.reactionAvatar = reactionAvatar;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public boolean isReactionNegative() {
        return reactionNegative;
    }

    public void setReactionNegative(boolean reactionNegative) {
        this.reactionNegative = reactionNegative;
    }

    public int getReactionEmoji() {
        return reactionEmoji;
    }

    public void setReactionEmoji(int reactionEmoji) {
        this.reactionEmoji = reactionEmoji;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingOwnerName", postingOwnerName);
        model.put("postingOwnerFullName", postingOwnerFullName);
        model.put("postingOwnerAvatar", postingOwnerAvatar);
        model.put("postingId", postingId);
        model.put("commentId", commentId);
        model.put("reactionNodeName", reactionNodeName);
        model.put("reactionFullName", reactionFullName);
        model.put("reactionAvatar", reactionAvatar);
        model.put("commentHeading", commentHeading);
        model.put("reactionNegative", reactionNegative);
        model.put("reactionEmoji", reactionEmoji);
    }

}
