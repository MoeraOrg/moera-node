package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.MentionPostingInstants;
import org.moera.node.instant.PostingInstants;
import org.moera.node.instant.RemoteCommentInstants;
import org.moera.node.instant.ReplyCommentInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.ForeignCommentAddedLiberin;
import org.moera.node.liberin.model.ForeignCommentDeletedLiberin;
import org.moera.node.liberin.model.MentionInRemotePostingAddedLiberin;
import org.moera.node.liberin.model.MentionInRemotePostingDeletedLiberin;
import org.moera.node.liberin.model.RemotePostingAddedLiberin;
import org.moera.node.liberin.model.RemotePostingAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingCommentsSubscribeFailedLiberin;
import org.moera.node.liberin.model.RemotePostingDeletedLiberin;
import org.moera.node.liberin.model.RemotePostingImportantUpdateLiberin;
import org.moera.node.liberin.model.RemotePostingUpdateFailedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.liberin.model.RemotePostingVerificationFailedLiberin;
import org.moera.node.liberin.model.RemotePostingVerifiedLiberin;
import org.moera.node.liberin.model.ReplyCommentAddedLiberin;
import org.moera.node.liberin.model.ReplyCommentDeletedLiberin;
import org.moera.node.model.event.RemotePostingAddedEvent;
import org.moera.node.model.event.RemotePostingUpdatedEvent;
import org.moera.node.model.event.RemotePostingVerificationFailedEvent;
import org.moera.node.model.event.RemotePostingVerifiedEvent;

@LiberinReceptor
public class RemotePostingReceptor extends LiberinReceptorBase {

    @Inject
    private PostingInstants postingInstants;

    @Inject
    private MentionPostingInstants mentionPostingInstants;

    @Inject
    private RemoteCommentInstants remoteCommentInstants;

    @Inject
    private ReplyCommentInstants replyCommentInstants;

    @LiberinMapping
    public void added(RemotePostingAddedLiberin liberin) {
        send(liberin, new RemotePostingAddedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void addingFailed(RemotePostingAddingFailedLiberin liberin) {
        postingInstants.remoteAddingFailed(liberin.getNodeInfo());
    }

    @LiberinMapping
    public void updated(RemotePostingUpdatedLiberin liberin) {
        send(liberin, new RemotePostingUpdatedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void updateFailed(RemotePostingUpdateFailedLiberin liberin) {
        postingInstants.remoteUpdateFailed(liberin.getNodeInfo(), liberin.getPostingId(), liberin.getPrevPostingInfo());
    }

    @LiberinMapping
    public void importantUpdate(RemotePostingImportantUpdateLiberin liberin) {
        postingInstants.updated(liberin.getNodeName(), liberin.getOwnerName(), liberin.getOwnerFullName(),
                liberin.getOwnerGender(), liberin.getOwnerAvatar(), liberin.getId(), liberin.getHeading(),
                liberin.getDescription());
    }

    @LiberinMapping
    public void deleted(RemotePostingDeletedLiberin liberin) {
        send(liberin, new RemotePostingUpdatedEvent(liberin.getNodeName(), liberin.getPostingId()));
    }

    @LiberinMapping
    public void commentsSubscribeFailed(RemotePostingCommentsSubscribeFailedLiberin liberin) {
        postingInstants.subscribingToCommentsFailed(liberin.getNodeName(), liberin.getPostingId(),
                liberin.getPostingInfo());
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
        mentionPostingInstants.added(liberin.getNodeName(), liberin.getOwnerName(), liberin.getOwnerFullName(),
                liberin.getOwnerGender(), liberin.getOwnerAvatar(), liberin.getId(), liberin.getHeading(),
                liberin.getSheriffs(), liberin.getSheriffMarks());
    }

    @LiberinMapping
    public void mentionDeleted(MentionInRemotePostingDeletedLiberin liberin) {
        mentionPostingInstants.deleted(liberin.getNodeName(), liberin.getPostingId());
    }

    @LiberinMapping
    public void foreignCommentAdded(ForeignCommentAddedLiberin liberin) {
        remoteCommentInstants.added(liberin.getNodeName(), liberin.getPostingOwnerName(),
                liberin.getPostingOwnerFullName(), liberin.getPostingOwnerGender(), liberin.getPostingOwnerAvatar(),
                liberin.getPostingId(), liberin.getPostingHeading(), liberin.getPostingSheriffs(),
                liberin.getPostingSheriffMarks(), liberin.getCommentOwnerName(), liberin.getCommentOwnerFullName(),
                liberin.getCommentOwnerGender(), liberin.getCommentOwnerAvatar(), liberin.getCommentId(),
                liberin.getCommentHeading(), liberin.getCommentSheriffMarks(), liberin.getSubscriptionReason());
    }

    @LiberinMapping
    public void foreignCommentDeleted(ForeignCommentDeletedLiberin liberin) {
        remoteCommentInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getOwnerName(),
                liberin.getCommentId(), liberin.getReason());
    }

    @LiberinMapping
    public void replyCommentAdded(ReplyCommentAddedLiberin liberin) {
        replyCommentInstants.added(liberin.getNodeName(), liberin.getPostingOwnerName(),
                liberin.getPostingOwnerFullName(), liberin.getPostingOwnerGender(), liberin.getPostingOwnerAvatar(),
                liberin.getPostingHeading(), liberin.getPostingSheriffs(), liberin.getPostingSheriffMarks(),
                liberin.getPostingId(), liberin.getCommentOwnerName(), liberin.getCommentOwnerFullName(),
                liberin.getCommentOwnerGender(), liberin.getCommentOwnerAvatar(), liberin.getCommentSheriffMarks(),
                liberin.getCommentId(), liberin.getRepliedToHeading(), liberin.getRepliedToId());
    }

    @LiberinMapping
    public void replyCommentDeleted(ReplyCommentDeletedLiberin liberin) {
        replyCommentInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId(),
                liberin.getCommentOwnerName());
    }

}
