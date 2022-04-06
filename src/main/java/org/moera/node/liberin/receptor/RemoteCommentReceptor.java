package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteCommentAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerifiedLiberin;
import org.moera.node.model.event.RemoteCommentAddedEvent;
import org.moera.node.model.event.RemoteCommentUpdatedEvent;
import org.moera.node.model.event.RemoteCommentVerificationFailedEvent;
import org.moera.node.model.event.RemoteCommentVerifiedEvent;

@LiberinReceptor
public class RemoteCommentReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(RemoteCommentAddedLiberin liberin) {
        send(liberin,
                new RemoteCommentAddedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId()));
    }

    @LiberinMapping
    public void updated(RemoteCommentUpdatedLiberin liberin) {
        send(liberin,
                new RemoteCommentUpdatedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId()));
    }

    @LiberinMapping
    public void verified(RemoteCommentVerifiedLiberin liberin) {
        send(liberin, new RemoteCommentVerifiedEvent(liberin.getData()));
    }

    @LiberinMapping
    public void verificationFailed(RemoteCommentVerificationFailedLiberin liberin) {
        send(liberin, new RemoteCommentVerificationFailedEvent(liberin.getData()));
    }

}
