package org.moera.node.liberin.receptor;

import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FriendshipUpdatedLiberin;
import org.moera.node.model.ContactInfo;
import org.moera.node.model.FriendGroupDetails;
import org.moera.node.model.FriendInfo;
import org.moera.node.model.event.FriendshipUpdatedEvent;
import org.moera.node.model.notification.FriendshipUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class FriendshipReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(FriendshipUpdatedLiberin liberin) {
        List<FriendGroupDetails> friendGroups = liberin.getFriendGroups();
        if (friendGroups != null) {
            friendGroups = friendGroups.stream().map(FriendGroupDetails::toNonAdmin).collect(Collectors.toList());
        }
        send(Directions.single(liberin.getNodeId(), liberin.getFriendNodeName()),
                new FriendshipUpdatedNotification(friendGroups));
        FriendInfo friend = new FriendInfo(
                liberin.getFriendNodeName(),
                new ContactInfo(liberin.getContact(), universalContext.getOptions()),
                friendGroups
        );
        send(liberin, new FriendshipUpdatedEvent(friend));
    }

}
