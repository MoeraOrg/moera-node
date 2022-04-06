package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemotePostingAddedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.liberin.model.RemotePostingVerificationFailedLiberin;
import org.moera.node.liberin.model.RemotePostingVerifiedLiberin;
import org.moera.node.model.event.RemotePostingAddedEvent;
import org.moera.node.model.event.RemotePostingUpdatedEvent;
import org.moera.node.model.event.RemotePostingVerificationFailedEvent;
import org.moera.node.model.event.RemotePostingVerifiedEvent;

@LiberinReceptor
public class RemotePostingReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(RemotePostingAddedLiberin liberin) {
        send(liberin, new RemotePostingAddedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void updated(RemotePostingUpdatedLiberin liberin) {
        send(liberin, new RemotePostingUpdatedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void verified(RemotePostingVerifiedLiberin liberin) {
        send(liberin, new RemotePostingVerifiedEvent(liberin.getData()));
    }

    @LiberinMapping
    public void verificationFailed(RemotePostingVerificationFailedLiberin liberin) {
        send(liberin, new RemotePostingVerificationFailedEvent(liberin.getData()));
    }

}
