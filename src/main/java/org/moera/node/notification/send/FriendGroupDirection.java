package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.auth.principal.PrincipalFilter;

class FriendGroupDirection extends Direction {

    private UUID friendGroupId;

    FriendGroupDirection(UUID nodeId, UUID friendGroupId) {
        super(nodeId);
        this.friendGroupId = friendGroupId;
    }

    FriendGroupDirection(UUID nodeId, UUID friendGroupId, PrincipalFilter principalFilter) {
        super(nodeId, principalFilter);
        this.friendGroupId = friendGroupId;
    }

    public UUID getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(UUID friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

}
