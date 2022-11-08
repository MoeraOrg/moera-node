package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.FriendGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupInfo {

    private String id;
    private String title;
    private boolean visible;

    public FriendGroupInfo() {
    }

    public FriendGroupInfo(FriendGroup friendGroup) {
        id = friendGroup.getId().toString();
        title = friendGroup.getTitle();
        visible = friendGroup.isVisible();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
