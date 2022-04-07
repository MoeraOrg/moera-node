package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.CommentReactionInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteCommentReactionAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedLiberin;

@LiberinReceptor
public class RemoteCommentReactionReceptor extends LiberinReceptorBase {

    @Inject
    private CommentReactionInstants commentReactionInstants;

    @LiberinMapping
    public void added(RemoteCommentReactionAddedLiberin liberin) {
        commentReactionInstants.added(liberin.getNodeName(), liberin.getFullName(), liberin.getAvatar(),
                liberin.getPostingId(), liberin.getCommentId(), liberin.getOwnerName(), liberin.getOwnerFullName(),
                liberin.getOwnerAvatar(), liberin.getCommentHeading(), liberin.isNegative(), liberin.getEmoji());
    }

    @LiberinMapping
    public void addingFailed(RemoteCommentReactionAddingFailedLiberin liberin) {
        commentReactionInstants.addingFailed(liberin.getPostingId(), liberin.getPostingInfo(), liberin.getCommentId(),
                liberin.getCommentInfo());
    }

    @LiberinMapping
    public void deleted(RemoteCommentReactionDeletedLiberin liberin) {
        commentReactionInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId(),
                liberin.getOwnerName(), liberin.isNegative());
    }

    @LiberinMapping
    public void deletedAll(RemoteCommentReactionDeletedAllLiberin liberin) {
        commentReactionInstants.deletedAll(liberin.getNodeName(), liberin.getPostingId(), liberin.getCommentId());
    }

}
