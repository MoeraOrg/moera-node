package org.moera.node.model.notification;

import java.util.List;

import javax.validation.Valid;

import org.moera.lib.util.LogUtil;
import org.moera.node.model.FriendGroupInfo;
import org.springframework.data.util.Pair;

public class FriendGroupUpdatedNotification extends Notification {

    @Valid
    private FriendGroupInfo friendGroup;

    public FriendGroupUpdatedNotification() {
        super(NotificationType.FRIEND_GROUP_UPDATED);
    }

    public FriendGroupUpdatedNotification(FriendGroupInfo friendGroup) {
        super(NotificationType.FRIEND_GROUP_UPDATED);
        this.friendGroup = friendGroup;
    }

    public FriendGroupInfo getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroupInfo friendGroup) {
        this.friendGroup = friendGroup;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("friendGroupId", LogUtil.format(friendGroup.getId())));
    }

}
