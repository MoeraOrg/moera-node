package org.moera.node.model.notification;

import java.util.Collection;

import jakarta.validation.Valid;

import org.moera.lib.node.types.FriendGroupDetails;

public class FriendshipUpdatedNotification extends Notification {

    @Valid
    private FriendGroupDetails[] friendGroups;

    public FriendshipUpdatedNotification() {
        super(NotificationType.FRIENDSHIP_UPDATED);
    }

    public FriendshipUpdatedNotification(Collection<FriendGroupDetails> friendGroups) {
        super(NotificationType.FRIENDSHIP_UPDATED);
        this.friendGroups = friendGroups != null
                ? friendGroups.toArray(FriendGroupDetails[]::new)
                : new FriendGroupDetails[0];
    }

    public FriendGroupDetails[] getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(FriendGroupDetails[] friendGroups) {
        this.friendGroups = friendGroups;
    }

}
