package org.moera.node.liberin.receptor;

import org.moera.node.data.Posting;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.PostingImportantUpdateNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class PostingReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(PostingAddedLiberin liberin) {
        send(liberin, new PostingAddedEvent(liberin.getPosting()));

        if (liberin.getPublications() != null) {
            liberin.getPublications().stream()
                    .map(StoryAttributes::getFeedName)
                    .forEach(fn -> send(Directions.feedSubscribers(liberin.getNodeId(), fn),
                            new FeedPostingAddedNotification(fn, liberin.getPosting().getId())));
        }
    }

    @LiberinMapping
    public void updated(PostingUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingUpdatedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingUpdatedNotification(posting.getId()));
        if (posting.getCurrentRevision().isUpdateImportant()) {
            send(Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId()),
                    new PostingImportantUpdateNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            posting.getCurrentRevision().getUpdateDescription()));
        }
    }

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingRestoredEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingUpdatedNotification(posting.getId()));
    }

}
