package org.moera.node.model;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.data.Friend;
import org.moera.node.data.MediaFile;

public class FriendDescription {

    @NotBlank
    @Size(max = 63)
    private String nodeName;

    @Size(max = 96)
    private String fullName;

    @Size(max = 31)
    private String gender;

    @Valid
    private AvatarDescription avatar;

    @JsonIgnore
    private MediaFile avatarMediaFile;

    private List<FriendGroupAssignment> groups;

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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public AvatarDescription getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarDescription avatar) {
        this.avatar = avatar;
    }

    public MediaFile getAvatarMediaFile() {
        return avatarMediaFile;
    }

    public void setAvatarMediaFile(MediaFile avatarMediaFile) {
        this.avatarMediaFile = avatarMediaFile;
    }

    public List<FriendGroupAssignment> getGroups() {
        return groups;
    }

    public void setGroups(List<FriendGroupAssignment> groups) {
        this.groups = groups;
    }

    public void toFriend(Friend friend) {
        friend.setRemoteNodeName(nodeName);
        friend.setRemoteFullName(fullName);
        friend.setRemoteGender(gender);
        if (avatar != null) {
            if (avatarMediaFile != null) {
                friend.setRemoteAvatarMediaFile(avatarMediaFile);
            }
            if (avatar.getShape() != null) {
                friend.setRemoteAvatarShape(avatar.getShape());
            }
        }
    }

}
