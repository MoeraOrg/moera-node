package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FriendshipUpdatedLiberin;
import org.moera.node.model.event.FriendshipUpdatedEvent;

@LiberinReceptor
public class FriendshipReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(FriendshipUpdatedLiberin liberin) {
        send(liberin, new FriendshipUpdatedEvent(liberin.getFriendNodeName(), liberin.getFriendGroups()));
    }

}
