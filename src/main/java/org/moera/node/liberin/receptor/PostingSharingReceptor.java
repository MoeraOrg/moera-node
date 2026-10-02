package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.EntrySharingRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.instant.PostingSharingInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingSharingAddedLiberin;
import org.moera.node.liberin.model.PostingSharingDeletedLiberin;
import org.moera.node.liberin.model.PostingTotalSharingsUpdatedLiberin;
import org.moera.node.model.SharingInfoUtil;
import org.moera.node.model.event.PostingSharingAddedEvent;
import org.moera.node.model.event.PostingSharingDeletedEvent;
import org.moera.node.model.event.PostingTotalSharingsChangedEvent;
import org.moera.node.model.notification.PostingSharingsUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class PostingSharingReceptor extends LiberinReceptorBase {

    @Inject
    private PostingSharingInstants postingSharingInstants;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntrySharingRepository entrySharingRepository;

    @LiberinMapping
    public void added(PostingSharingAddedLiberin liberin) {
        postingRepository.findByNodeIdAndIdForUpdate(liberin.getNodeId(), liberin.getPosting().getId())
            .ifPresent(posting -> entrySharingRepository.findByEntryIdAndId(posting.getId(), liberin.getSharing().getId())
                .ifPresent(sharing -> {
                    postingSharingInstants.added(posting, sharing);
                    send(liberin, new PostingSharingAddedEvent(
                        posting.getId(), SharingInfoUtil.build(sharing, directServeOperations),
                        posting.getViewE().a().and(posting.getViewSharingsE())
                    ));
                }));
    }

    @LiberinMapping
    public void deleted(PostingSharingDeletedLiberin liberin) {
        postingSharingInstants.deleted(liberin.getPosting().getId(), liberin.getSubstoryIds());
        Posting posting = liberin.getPosting();
        send(liberin, new PostingSharingDeletedEvent(
            posting.getId(), liberin.getSharing().getId(), posting.getViewE().a().and(posting.getViewSharingsE())
        ));
    }

    @LiberinMapping
    public void totalUpdated(PostingTotalSharingsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();
        Principal viewTotalSharings = posting.isOriginal()
            ? posting.getViewTotalSharingsE()
            : posting.getReceiverViewTotalSharingsE();
        PrincipalFilter filter = posting.getViewE().a().and(viewTotalSharings);
        send(liberin, new PostingTotalSharingsChangedEvent(posting, liberin.getTotal(), filter));
        if (posting.isOriginal()) {
            send(
                Directions.postingSubscribers(posting.getNodeId(), posting.getId(), filter),
                PostingSharingsUpdatedNotificationUtil.build(posting.getId(), liberin.getTotal())
            );
        }
    }

}
