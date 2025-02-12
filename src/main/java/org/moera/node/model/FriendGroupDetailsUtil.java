package org.moera.node.model;

import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.util.Util;

public class FriendGroupDetailsUtil {

    public static FriendGroupDetails build(String id, String title, Long addedAt) {
        FriendGroupDetails details = new FriendGroupDetails();
        details.setId(id);
        details.setTitle(title);
        details.setAddedAt(addedAt);
        return details;
    }

    public static FriendGroupDetails build(Friend friend, boolean isAdmin) {
        FriendGroupDetails details = new FriendGroupDetails();
        details.setId(friend.getFriendGroup().getId().toString());
        if (isAdmin || !friend.getFriendGroup().getViewPrincipal().isAdmin()) {
            details.setTitle(friend.getFriendGroup().getTitle());
        }
        details.setAddedAt(Util.toEpochSecond(friend.getCreatedAt()));

        FriendOperations operations = new FriendOperations();
        operations.setView(friend.getViewPrincipal(), Principal.PUBLIC);
        details.setOperations(operations);

        return details;
    }

    public static FriendGroupDetails build(FriendOf friendOf) {
        FriendGroupDetails details = new FriendGroupDetails();
        details.setId(friendOf.getRemoteGroupId());
        details.setTitle(friendOf.getRemoteGroupTitle());
        details.setAddedAt(Util.toEpochSecond(friendOf.getRemoteAddedAt()));
        return details;
    }

    public static FriendGroupDetails toNonAdmin(FriendGroupDetails details) {
        Principal viewPrincipal = FriendOperations.getView(details.getOperations(), Principal.PUBLIC);
        if (viewPrincipal.isAdmin()) {
            FriendGroupDetails friendGroupDetails = details.clone();
            friendGroupDetails.setTitle(null);
            return friendGroupDetails;
        }
        return details;
    }

}
