package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.model.event.RemotePostingUpdatedEvent;

@LiberinReceptor
public class RemotePostingReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(RemotePostingUpdatedLiberin liberin) {
        send(liberin, new RemotePostingUpdatedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

}
