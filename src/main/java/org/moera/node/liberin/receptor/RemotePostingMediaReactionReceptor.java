package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.PostingMediaReactionInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedLiberin;

@LiberinReceptor
public class RemotePostingMediaReactionReceptor extends LiberinReceptorBase {

    @Inject
    private PostingMediaReactionInstants postingMediaReactionInstants;

    @LiberinMapping
    public void added(RemotePostingMediaReactionAddedLiberin liberin) {
        postingMediaReactionInstants.added(liberin.getNodeName(), liberin.getParentPostingNodeName(),
                liberin.getParentPostingFullName(), liberin.getParentPostingAvatar(), liberin.getPostingId(),
                liberin.getParentPostingId(), liberin.getParentMediaId(), liberin.getReactionNodeName(),
                liberin.getReactionFullName(), liberin.getReactionAvatar(), liberin.getParentPostingHeading(),
                liberin.isReactionNegative(), liberin.getReactionEmoji());
    }

    @LiberinMapping
    public void addingFailed(RemotePostingMediaReactionAddingFailedLiberin liberin) {
        postingMediaReactionInstants.addingFailed(liberin.getNodeName(), liberin.getPostingId(),
                liberin.getParentPostingId(), liberin.getParentMediaId(), liberin.getParentPostingInfo());
    }

    @LiberinMapping
    public void deleted(RemotePostingMediaReactionDeletedLiberin liberin) {
        postingMediaReactionInstants.deleted(liberin.getNodeName(), liberin.getPostingId(), liberin.getOwnerName(),
                liberin.isNegative());
    }

    @LiberinMapping
    public void deletedAll(RemotePostingMediaReactionDeletedAllLiberin liberin) {
        postingMediaReactionInstants.deletedAll(liberin.getNodeName(), liberin.getPostingId());
    }

}
