package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.MentionPostingInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.MentionInRemotePostingAddedLiberin;
import org.moera.node.liberin.model.MentionInRemotePostingDeletedLiberin;
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

    @Inject
    private MentionPostingInstants mentionPostingInstants;

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

    @LiberinMapping
    public void mentionAdded(MentionInRemotePostingAddedLiberin liberin) {
        mentionPostingInstants.added(liberin.getNodeName(), liberin.getFullName(), liberin.getAvatar(),
                liberin.getPostingId(), liberin.getPostingHeading());
    }

    @LiberinMapping
    public void mentionDeleted(MentionInRemotePostingDeletedLiberin liberin) {
        mentionPostingInstants.deleted(liberin.getNodeName(), liberin.getPostingId());
    }

}
