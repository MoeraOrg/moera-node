package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class MentionInRemotePostingAddedLiberin extends Liberin {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private String postingId;
    private String postingHeading;

    public MentionInRemotePostingAddedLiberin(String nodeName, String fullName, AvatarImage avatar, String postingId,
                                              String postingHeading) {
        this.nodeName = nodeName;
        this.fullName = fullName;
        this.avatar = avatar;
        this.postingId = postingId;
        this.postingHeading = postingHeading;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
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

}
