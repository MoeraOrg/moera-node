package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteNodeAskedLiberin;
import org.moera.node.model.notification.AskedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class RemoteAskReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void asked(RemoteNodeAskedLiberin liberin) {
        send(
            Directions.single(universalContext.nodeId(), liberin.getNodeName()),
            AskedNotificationUtil.build(liberin.getAskDescription())
        );
    }

}
