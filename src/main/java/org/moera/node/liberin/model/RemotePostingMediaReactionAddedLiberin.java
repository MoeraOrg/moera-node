package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.liberin.Liberin;

public class RemotePostingMediaReactionAddedLiberin extends Liberin {

    private String nodeName;
    private String parentPostingNodeName;
    private String parentPostingFullName;
    private String parentPostingGender;
    private AvatarImage parentPostingAvatar;
    private String postingId;
    private String parentPostingId;
    private String parentMediaId;
    private String reactionNodeName;
    private String reactionFullName;
    private String reactionGender;
    private AvatarImage reactionAvatar;
    private String parentPostingHeading;
    private boolean reactionNegative;
    private int reactionEmoji;

    public RemotePostingMediaReactionAddedLiberin(String nodeName, String parentPostingNodeName,
                                                  String parentPostingFullName, String parentPostingGender,
                                                  AvatarImage parentPostingAvatar, String postingId,
                                                  String parentPostingId, String parentMediaId, String reactionNodeName,
                                                  String reactionFullName, String reactionGender,
                                                  AvatarImage reactionAvatar, String parentPostingHeading,
                                                  boolean reactionNegative, int reactionEmoji) {
        this.nodeName = nodeName;
        this.parentPostingNodeName = parentPostingNodeName;
        this.parentPostingFullName = parentPostingFullName;
        this.parentPostingGender = parentPostingGender;
        this.parentPostingAvatar = parentPostingAvatar;
        this.postingId = postingId;
        this.parentPostingId = parentPostingId;
        this.parentMediaId = parentMediaId;
        this.reactionNodeName = reactionNodeName;
        this.reactionFullName = reactionFullName;
        this.reactionGender = reactionGender;
        this.reactionAvatar = reactionAvatar;
        this.parentPostingHeading = parentPostingHeading;
        this.reactionNegative = reactionNegative;
        this.reactionEmoji = reactionEmoji;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getParentPostingNodeName() {
        return parentPostingNodeName;
    }

    public void setParentPostingNodeName(String parentPostingNodeName) {
        this.parentPostingNodeName = parentPostingNodeName;
    }

    public String getParentPostingFullName() {
        return parentPostingFullName;
    }

    public void setParentPostingFullName(String parentPostingFullName) {
        this.parentPostingFullName = parentPostingFullName;
    }

    public String getParentPostingGender() {
        return parentPostingGender;
    }

    public void setParentPostingGender(String parentPostingGender) {
        this.parentPostingGender = parentPostingGender;
    }

    public AvatarImage getParentPostingAvatar() {
        return parentPostingAvatar;
    }

    public void setParentPostingAvatar(AvatarImage parentPostingAvatar) {
        this.parentPostingAvatar = parentPostingAvatar;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(String parentPostingId) {
        this.parentPostingId = parentPostingId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
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

    public String getReactionGender() {
        return reactionGender;
    }

    public void setReactionGender(String reactionGender) {
        this.reactionGender = reactionGender;
    }

    public AvatarImage getReactionAvatar() {
        return reactionAvatar;
    }

    public void setReactionAvatar(AvatarImage reactionAvatar) {
        this.reactionAvatar = reactionAvatar;
    }

    public String getParentPostingHeading() {
        return parentPostingHeading;
    }

    public void setParentPostingHeading(String parentPostingHeading) {
        this.parentPostingHeading = parentPostingHeading;
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
        model.put("parentPostingNodeName", parentPostingNodeName);
        model.put("parentPostingFullName", parentPostingFullName);
        model.put("parentPostingAvatar", parentPostingAvatar);
        model.put("postingId", postingId);
        model.put("parentPostingId", parentPostingId);
        model.put("parentMediaId", parentMediaId);
        model.put("reactionNodeName", reactionNodeName);
        model.put("reactionFullName", reactionFullName);
        model.put("reactionAvatar", reactionAvatar);
        model.put("parentPostingHeading", parentPostingHeading);
        model.put("reactionNegative", reactionNegative);
        model.put("reactionEmoji", reactionEmoji);
    }

}
