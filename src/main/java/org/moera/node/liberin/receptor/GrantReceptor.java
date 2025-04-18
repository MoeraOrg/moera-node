package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.GrantUpdatedLiberin;
import org.moera.node.model.GrantInfoUtil;
import org.moera.node.model.event.GrantUpdatedEvent;
import org.moera.node.model.notification.GrantUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class GrantReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void grantUpdated(GrantUpdatedLiberin liberin) {
        send(liberin, new GrantUpdatedEvent(GrantInfoUtil.build(liberin.getNodeName(), liberin.getScope())));
        send(
            Directions.single(liberin.getNodeId(), liberin.getNodeName()),
            GrantUpdatedNotificationUtil.build(liberin.getScope())
        );
    }

}
