package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.CommentInstants;
import org.moera.node.instant.MentionCommentInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.MentionInRemoteCommentAddedLiberin;
import org.moera.node.liberin.model.MentionInRemoteCommentDeletedLiberin;
import org.moera.node.liberin.model.RemoteCommentAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentAddingFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdateFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerifiedLiberin;
import org.moera.node.model.event.RemoteCommentAddedEvent;
import org.moera.node.model.event.RemoteCommentUpdatedEvent;
import org.moera.node.model.event.RemoteCommentVerificationFailedEvent;
import org.moera.node.model.event.RemoteCommentVerifiedEvent;

@LiberinReceptor
public class RemoteCommentReceptor extends LiberinReceptorBase {

    @Inject
    private CommentInstants commentInstants;

    @Inject
    private MentionCommentInstants mentionCommentInstants;

    @LiberinMapping
    public void added(RemoteCommentAddedLiberin liberin) {
        send(liberin,
                new RemoteCommentAddedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId()));
    }

    @LiberinMapping
    public void addingFailed(RemoteCommentAddingFailedLiberin liberin) {
        commentInstants.addingFailed(liberin.getPostingId(), liberin.getPostingInfo());
    }

    @LiberinMapping
    public void updated(RemoteCommentUpdatedLiberin liberin) {
        send(liberin,
                new RemoteCommentUpdatedEvent(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId()));
    }

    @LiberinMapping
    public void updateFailed(RemoteCommentUpdateFailedLiberin liberin) {
        commentInstants.updateFailed(liberin.getPostingId(), liberin.getPostingInfo(), liberin.getCommentId(),
                liberin.getPrevCommentInfo());
    }

    @LiberinMapping
    public void verified(RemoteCommentVerifiedLiberin liberin) {
        send(liberin, new RemoteCommentVerifiedEvent(liberin.getData()));
    }

    @LiberinMapping
    public void verificationFailed(RemoteCommentVerificationFailedLiberin liberin) {
        send(liberin, new RemoteCommentVerificationFailedEvent(liberin.getData()));
    }

    @LiberinMapping
    public void mentionAdded(MentionInRemoteCommentAddedLiberin liberin) {
        mentionCommentInstants.added(liberin.getNodeName(), liberin.getFullName(), liberin.getAvatar(),
                liberin.getPostingId(), liberin.getPostingHeading(), liberin.getOwnerName(), liberin.getOwnerFullName(),
                liberin.getOwnerAvatar(), liberin.getCommentId(), liberin.getCommentHeading());
    }

    @LiberinMapping
    public void mentionDeleted(MentionInRemoteCommentDeletedLiberin liberin) {
        mentionCommentInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId());
    }

}
