package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemotePostingReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingReactionDeletedLiberin;
import org.moera.node.liberin.model.RemoteReactionVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteReactionVerifiedLiberin;
import org.moera.node.model.event.RemoteReactionAddedEvent;
import org.moera.node.model.event.RemoteReactionDeletedEvent;
import org.moera.node.model.event.RemoteReactionVerificationFailedEvent;
import org.moera.node.model.event.RemoteReactionVerifiedEvent;

@LiberinReceptor
public class RemotePostingReactionReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void deleted(RemotePostingReactionAddedLiberin liberin) {
        send(liberin,
                new RemoteReactionAddedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getReactionInfo()));
    }

    @LiberinMapping
    public void deleted(RemotePostingReactionDeletedLiberin liberin) {
        send(liberin, new RemoteReactionDeletedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void verified(RemoteReactionVerifiedLiberin liberin) {
        send(liberin, new RemoteReactionVerifiedEvent(liberin.getData()));
    }

    @LiberinMapping
    public void verificationFailed(RemoteReactionVerificationFailedLiberin liberin) {
        send(liberin, new RemoteReactionVerificationFailedEvent(liberin.getData()));
    }

}
