package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.node.instant.CommentMediaReactionInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedLiberin;

@LiberinReceptor
public class RemoteCommentMediaReactionReceptor extends LiberinReceptorBase {

    @Inject
    private CommentMediaReactionInstants commentMediaReactionInstants;

    @LiberinMapping
    public void added(RemoteCommentMediaReactionAddedLiberin liberin) {
        commentMediaReactionInstants.added(liberin.getNodeName(), liberin.getParentPostingNodeName(),
                liberin.getParentPostingFullName(), liberin.getParentPostingGender(), liberin.getParentPostingAvatar(),
                liberin.getMediaPostingId(), liberin.getParentPostingId(), liberin.getParentCommentId(),
                liberin.getParentMediaId(), liberin.getReactionNodeName(), liberin.getReactionFullName(),
                liberin.getReactionGender(), liberin.getReactionAvatar(), liberin.getCommentHeading(),
                liberin.isReactionNegative(), liberin.getReactionEmoji());
    }

    @LiberinMapping
    public void deleted(RemoteCommentMediaReactionDeletedLiberin liberin) {
        commentMediaReactionInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getOwnerName(),
                liberin.isNegative());
    }

    @LiberinMapping
    public void deletedAll(RemoteCommentMediaReactionDeletedAllLiberin liberin) {
        commentMediaReactionInstants.deletedAll(liberin.getNodeName(), liberin.getPostingId());
    }

    @LiberinMapping
    public void addingFailed(RemoteCommentMediaReactionAddingFailedLiberin liberin) {
        String parentPostingId = liberin.getParentCommentInfo().getPostingId();
        String parentCommentId = liberin.getParentCommentInfo().getId();
        commentMediaReactionInstants.addingFailed(liberin.getNodeName(), liberin.getMediaPostingId(), parentPostingId,
                parentCommentId, liberin.getParentMediaId(), liberin.getParentPostingInfo(),
                liberin.getParentCommentInfo());
    }

}
