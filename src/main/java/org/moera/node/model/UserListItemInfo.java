package org.moera.node.model;

import org.moera.node.data.UserListItem;
import org.moera.node.util.Util;

public class UserListItemInfo {

    private String nodeName;
    private long createdAt;
    private Long moment;

    public UserListItemInfo() {
    }

    public UserListItemInfo(UserListItem userListItem) {
        nodeName = userListItem.getNodeName();
        createdAt = Util.toEpochSecond(userListItem.getCreatedAt());
        moment = userListItem.getMoment();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

}
