package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.GrantUpdatedLiberin;
import org.moera.node.model.GrantInfo;
import org.moera.node.model.event.GrantUpdatedEvent;
import org.moera.node.model.notification.GrantUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class GrantReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void grantUpdated(GrantUpdatedLiberin liberin) {
        send(liberin, new GrantUpdatedEvent(new GrantInfo(liberin.getNodeName(), liberin.getScope())));
        send(Directions.single(liberin.getNodeId(), liberin.getNodeName()),
                new GrantUpdatedNotification(liberin.getScope()));
    }

}
