package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.instant.SheriffInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SheriffComplainAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupUpdatedLiberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.event.SheriffComplainAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupUpdatedEvent;

@LiberinReceptor
public class SheriffComplainReceptor extends LiberinReceptorBase {

    @Inject
    private SheriffInstants sheriffInstants;

    @LiberinMapping
    public void groupAdded(SheriffComplainGroupAddedLiberin liberin) {
        send(liberin, new SheriffComplainGroupAddedEvent(liberin.getGroup()));
    }

    @LiberinMapping
    public void groupUpdated(SheriffComplainGroupUpdatedLiberin liberin) {
        send(liberin, new SheriffComplainGroupUpdatedEvent(liberin.getGroup()));
        if (liberin.getGroup().getStatus() == SheriffComplainStatus.PREPARED
                && liberin.getGroup().getStatus() != liberin.getPrevStatus()) {
            sheriffInstants.complainAdded(universalContext.nodeName(), new AvatarImage(universalContext.getAvatar()),
                    liberin.getGroup().getId().toString());
        }
    }

    @LiberinMapping
    public void added(SheriffComplainAddedLiberin liberin) {
        send(liberin, new SheriffComplainAddedEvent(liberin.getComplain(), liberin.getGroupId()));
    }

}
