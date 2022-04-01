package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemotePostingReactionDeletedLiberin;
import org.moera.node.model.event.RemoteReactionDeletedEvent;

@LiberinReceptor
public class RemotePostingReactionReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void deleted(RemotePostingReactionDeletedLiberin liberin) {
        send(liberin, new RemoteReactionDeletedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

}
