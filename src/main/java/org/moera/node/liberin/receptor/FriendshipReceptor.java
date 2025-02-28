package org.moera.node.liberin.receptor;

import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendInfo;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FriendshipUpdatedLiberin;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.FriendGroupDetailsUtil;
import org.moera.node.model.FriendInfoUtil;
import org.moera.node.model.event.FriendshipUpdatedEvent;
import org.moera.node.model.notification.FriendshipUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class FriendshipReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(FriendshipUpdatedLiberin liberin) {
        List<FriendGroupDetails> friendGroups = liberin.getFriendGroups();
        if (friendGroups != null) {
            friendGroups = friendGroups.stream().map(FriendGroupDetailsUtil::toNonAdmin).collect(Collectors.toList());
        }
        send(
            Directions.single(liberin.getNodeId(), liberin.getFriendNodeName()),
            FriendshipUpdatedNotificationUtil.build(friendGroups)
        );
        FriendInfo friend = FriendInfoUtil.build(
            liberin.getFriendNodeName(),
            ContactInfoUtil.build(liberin.getContact(), universalContext.getOptions()),
            friendGroups
        );
        send(liberin, new FriendshipUpdatedEvent(friend));
    }

}
