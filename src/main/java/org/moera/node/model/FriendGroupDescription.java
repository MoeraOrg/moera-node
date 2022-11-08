package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.FriendGroup;

public class FriendGroupDescription {

    @NotBlank
    @Size(max = 63)
    private String title;

    private Boolean visible;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public void toFriendGroup(FriendGroup friendGroup) {
        friendGroup.setTitle(title);
        if (visible != null) {
            friendGroup.setVisible(visible);
        }
    }

}
