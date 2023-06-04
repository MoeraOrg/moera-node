package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SheriffComplainAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupUpdatedLiberin;
import org.moera.node.model.event.SheriffComplainAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupUpdatedEvent;

@LiberinReceptor
public class SheriffComplainReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void groupAdded(SheriffComplainGroupAddedLiberin liberin) {
        send(liberin, new SheriffComplainGroupAddedEvent(liberin.getGroup()));
    }

    @LiberinMapping
    public void groupUpdated(SheriffComplainGroupUpdatedLiberin liberin) {
        send(liberin, new SheriffComplainGroupUpdatedEvent(liberin.getGroup()));
    }

    @LiberinMapping
    public void added(SheriffComplainAddedLiberin liberin) {
        send(liberin, new SheriffComplainAddedEvent(liberin.getComplain(), liberin.getGroupId()));
    }

}
