package org.moera.node.model;

import org.moera.lib.node.types.FriendGroupAssignment;
import org.moera.lib.node.types.FriendOperations;
import org.moera.node.data.Friend;

public class FriendGroupAssignmentUtil {

    public static void toFriend(FriendGroupAssignment assignment, Friend friend) {
        if (FriendOperations.getView(assignment.getOperations(), null) != null) {
            friend.setViewPrincipal(assignment.getOperations().getView());
        }
    }

}
