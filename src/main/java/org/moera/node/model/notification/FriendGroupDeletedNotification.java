package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class FriendGroupDeletedNotification extends Notification {

    private String friendGroupId;

    public FriendGroupDeletedNotification() {
        super(NotificationType.FRIEND_GROUP_DELETED);
    }

    public FriendGroupDeletedNotification(UUID friendGroupId) {
        super(NotificationType.FRIEND_GROUP_DELETED);
        this.friendGroupId = friendGroupId.toString();
    }

    public String getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(String friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("friendGroupId", LogUtil.format(friendGroupId)));
    }

}
