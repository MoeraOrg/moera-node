package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.EntryMediaDownloadedLiberin;
import org.moera.node.liberin.model.MediaTitleUpdatedLiberin;
import org.moera.node.model.notification.LeasedMediaTitleUpdatedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class MediaReceptor extends LiberinReceptorBase {

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @LiberinMapping
    public void titleUpdated(MediaTitleUpdatedLiberin liberin) {
        send(
            Directions.leases(liberin.getNodeId(), liberin.getMediaId()),
            LeasedMediaTitleUpdatedNotificationUtil.build(liberin.getMediaId(), liberin.getTitle())
        );
    }

    @LiberinMapping
    public void entryMediaDownloaded(EntryMediaDownloadedLiberin liberin) {
        if (liberin.getCommentId() != null) {
            commentMediaDownloaded(liberin);
        } else {
            postingMediaDownloaded(liberin);
        }
    }

    private void postingMediaDownloaded(EntryMediaDownloadedLiberin liberin) {
        Posting posting = postingRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId())
            .orElse(null);
        if (posting == null) {
            return;
        }

        send(
            Directions.searchSubscribers(liberin.getNodeId(), posting.getViewE()),
            SearchContentUpdatedNotificationUtil.buildPostingMediaUpdate(
                posting.getId(),
                liberin.getMediaId(),
                liberin.getRemoteMediaNodeName(),
                liberin.getRemoteMediaId(),
                liberin.getTitle(),
                liberin.getTextContent()
            )
        );
    }

    private void commentMediaDownloaded(EntryMediaDownloadedLiberin liberin) {
        Comment comment = commentRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getCommentId())
            .orElse(null);
        if (comment == null || comment.getPosting() == null) {
            return;
        }

        send(
            Directions.searchSubscribers(liberin.getNodeId(), commentVisibilityFilter(comment.getPosting(), comment)),
            SearchContentUpdatedNotificationUtil.buildCommentMediaUpdate(
                comment.getPosting().getId(),
                comment.getId(),
                liberin.getMediaId(),
                liberin.getRemoteMediaNodeName(),
                liberin.getRemoteMediaId(),
                liberin.getTitle(),
                liberin.getTextContent()
            )
        );
    }

    private PrincipalFilter commentVisibilityFilter(Entry posting, Comment comment) {
        return posting.getViewE().a().and(posting.getViewCommentsE()).and(comment.getViewE());
    }

}
