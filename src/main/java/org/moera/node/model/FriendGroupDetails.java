package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Friend;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupDetails {

    private String id;
    private String title;
    private Long addedAt;

    public FriendGroupDetails() {
    }

    public FriendGroupDetails(Friend friend) {
        id = friend.getFriendGroup().getId().toString();
        title = friend.getFriendGroup().getTitle();
        addedAt = Util.toEpochSecond(friend.getCreatedAt());
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

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }

}
