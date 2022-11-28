package org.moera.node.model;

import org.moera.node.data.FriendOf;
import org.moera.node.util.Util;

public class FriendOfInfo {

    private String remoteNodeName;
    private String remoteFullName;
    private String remoteGender;
    private AvatarImage remoteAvatar;
    private FriendGroupDetails[] groups;

    public FriendOfInfo() {
    }

    public FriendOfInfo(FriendOf friendOf) {
        remoteNodeName = friendOf.getRemoteNodeName();
        remoteFullName = friendOf.getRemoteFullName();
        remoteGender = friendOf.getRemoteGender();
        remoteAvatar = new AvatarImage(friendOf.getRemoteAvatarMediaFile(), friendOf.getRemoteAvatarShape());
        groups = new FriendGroupDetails[] {
                new FriendGroupDetails(
                        friendOf.getRemoteGroupId(),
                        friendOf.getRemoteGroupTitle(),
                        Util.toEpochSecond(friendOf.getRemoteAddedAt())
                )
        };
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemoteGender() {
        return remoteGender;
    }

    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    public AvatarImage getRemoteAvatar() {
        return remoteAvatar;
    }

    public void setRemoteAvatar(AvatarImage remoteAvatar) {
        this.remoteAvatar = remoteAvatar;
    }

    public FriendGroupDetails[] getGroups() {
        return groups;
    }

    public void setGroups(FriendGroupDetails[] groups) {
        this.groups = groups;
    }

}
