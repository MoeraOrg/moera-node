package org.moera.node.liberin.receptor;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingCommentTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.body.Body;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.event.PostingDeletedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.model.notification.PostingDeletedNotification;
import org.moera.node.model.notification.PostingImportantUpdateNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.text.MentionsExtractor;

@LiberinReceptor
public class PostingReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(PostingAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        // FIXME isOriginal?
        notifyMentioned(liberin, posting.getId(), posting.getCurrentRevision(), null, posting.getOwnerName());
        send(liberin, new PostingAddedEvent(posting));

        if (liberin.getPublications() != null) {
            liberin.getPublications().stream()
                    .map(StoryAttributes::getFeedName)
                    .forEach(fn -> send(Directions.feedSubscribers(liberin.getNodeId(), fn),
                            new FeedPostingAddedNotification(fn, posting.getId())));
        }
    }

    @LiberinMapping
    public void updated(PostingUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        // FIXME isOriginal?
        notifyMentioned(liberin, posting.getId(), posting.getCurrentRevision(), liberin.getLatestRevision(),
                posting.getOwnerName());
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
    public void deleted(PostingDeletedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(liberin, posting.getId(), null, liberin.getLatestRevision(), posting.getOwnerName());
        }
        send(liberin, new PostingDeletedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingDeletedNotification(posting.getId()));
    }

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        // FIXME isOriginal?
        notifyMentioned(liberin, posting.getId(), posting.getCurrentRevision(), null, posting.getOwnerName());
        send(liberin, new PostingRestoredEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingUpdatedNotification(posting.getId()));
    }

    private void notifyMentioned(Liberin liberin, UUID postingId, EntryRevision current, EntryRevision latest,
                                 String ownerName) {
        String currentHeading = current != null ? current.getHeading() : null;
        Set<String> currentMentions = current != null
                ? MentionsExtractor.extract(new Body(current.getBody()))
                : Collections.emptySet();
        Set<String> latestMentions = latest != null && latest.getSignature() != null
                ? MentionsExtractor.extract(new Body(latest.getBody()))
                : Collections.emptySet();
        currentMentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(liberin.getNodeId(), m))
                .forEach(d -> send(d, new MentionPostingAddedNotification(postingId, currentHeading)));
        latestMentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> !currentMentions.contains(m))
                .map(m -> Directions.single(liberin.getNodeId(), m))
                .forEach(d -> send(d, new MentionPostingDeletedNotification(postingId)));
    }

    @LiberinMapping
    public void commentTotalsUpdated(PostingCommentTotalsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingCommentsChangedEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingCommentsUpdatedNotification(posting.getId(), liberin.getTotal()));
    }

}
