package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.DeleteNodeCancelledLiberin;
import org.moera.node.liberin.model.DeleteNodeRequestedLiberin;
import org.moera.node.mail.DeleteNodeCancelledMail;
import org.moera.node.mail.DeleteNodeMail;
import org.moera.node.model.event.DeleteNodeStatusUpdatedEvent;

@LiberinReceptor
public class ProviderReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void deleteNodeRequested(DeleteNodeRequestedLiberin liberin) {
        sendToRoot(new DeleteNodeMail(universalContext.nodeName(), liberin.getMessage()));
        send(liberin, new DeleteNodeStatusUpdatedEvent(true));
    }

    @LiberinMapping
    public void deleteNodeCancelled(DeleteNodeCancelledLiberin liberin) {
        sendToRoot(new DeleteNodeCancelledMail(universalContext.nodeName()));
        send(liberin, new DeleteNodeStatusUpdatedEvent(false));
    }

}
