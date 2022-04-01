package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.model.event.RemoteCommentUpdatedEvent;

@LiberinReceptor
public class RemoteCommentReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(RemoteCommentUpdatedLiberin liberin) {
        send(liberin,
                new RemoteCommentUpdatedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId()));
    }

}
