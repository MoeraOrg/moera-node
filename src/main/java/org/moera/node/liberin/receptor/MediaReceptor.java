package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.MediaTitleUpdatedLiberin;
import org.moera.node.model.notification.LeasedMediaTitleUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class MediaReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void titleUpdated(MediaTitleUpdatedLiberin liberin) {
        send(
            Directions.leases(liberin.getNodeId(), liberin.getMediaId()),
            LeasedMediaTitleUpdatedNotificationUtil.build(liberin.getMediaId(), liberin.getTitle())
        );
    }

}
