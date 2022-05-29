package org.moera.node.liberin.receptor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalExpression;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingCommentTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
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

    @Inject
    private StoryRepository storyRepository;

    @LiberinMapping
    public void added(PostingAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(posting, posting.getCurrentRevision(), posting.getViewE(),
                    null, Principal.PUBLIC, posting.getOwnerName());
        }
        send(liberin, new PostingAddedEvent(posting, posting.getViewE()));
    }

    @LiberinMapping
    public void updated(PostingUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(posting, posting.getCurrentRevision(), posting.getViewE(),
                    liberin.getLatestRevision(), liberin.getLatestViewPrincipal(), posting.getOwnerName());
        }

        PrincipalExpression addedFilter = posting.getViewE().a()
                .andNot(liberin.getLatestViewPrincipal());
        send(liberin, new PostingAddedEvent(posting, addedFilter));
        List<Story> stories = storyRepository.findByEntryId(posting.getNodeId(), posting.getId());
        stories.forEach(story ->
                send(Directions.feedSubscribers(posting.getNodeId(), story.getFeedName(), addedFilter),
                        new FeedPostingAddedNotification(story.getFeedName(), posting.getId())));

        PrincipalExpression updatedFilter = posting.getViewE().a()
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new PostingUpdatedEvent(posting, updatedFilter));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
                new PostingUpdatedNotification(posting.getId()));
        if (posting.getCurrentRevision().isUpdateImportant()) {
            send(Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
                    new PostingImportantUpdateNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            posting.getCurrentRevision().getUpdateDescription()));
        }

        PrincipalExpression deletedFilter = posting.getViewE().not()
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new PostingDeletedEvent(posting, deletedFilter));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), deletedFilter),
                new PostingDeletedNotification(posting.getId()));
    }

    @LiberinMapping
    public void deleted(PostingDeletedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(posting, null, Principal.PUBLIC, liberin.getLatestRevision(),
                    posting.getViewE(), posting.getOwnerName());
        }
        send(liberin, new PostingDeletedEvent(posting, posting.getViewE()));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
                new PostingDeletedNotification(posting.getId()));
    }

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(posting, posting.getCurrentRevision(), posting.getViewE(),
                    null, Principal.PUBLIC, posting.getOwnerName());
        }
        send(liberin, new PostingRestoredEvent(posting, posting.getViewE()));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
                new PostingUpdatedNotification(posting.getId()));
    }

    private void notifyMentioned(Posting posting, EntryRevision current, Principal currentView, EntryRevision latest,
                                 Principal latestView, String ownerName) {
        String currentHeading = current != null ? current.getHeading() : null;
        Set<String> currentMentions = current != null
                ? filterMentions(MentionsExtractor.extract(new Body(current.getBody())), ownerName, currentView)
                : Collections.emptySet();
        Set<String> latestMentions = latest != null && latest.getSignature() != null
                ? filterMentions(MentionsExtractor.extract(new Body(latest.getBody())), ownerName, latestView)
                : Collections.emptySet();
        currentMentions.stream()
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d -> send(d, new MentionPostingAddedNotification(posting.getId(), currentHeading)));
        latestMentions.stream()
                .filter(m -> !currentMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d -> send(d, new MentionPostingDeletedNotification(posting.getId())));
    }

    private Set<String> filterMentions(Set<String> mentions, String ownerName, Principal view) {
        return mentions.stream()
                .filter(m -> !Objects.equals(ownerName, m))
                .filter(m -> !m.equals(":"))
                .filter(m -> view.includes(false, m))
                .collect(Collectors.toSet());
    }

    @LiberinMapping
    public void commentTotalsUpdated(PostingCommentTotalsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        PrincipalFilter viewFilter = posting.getViewE().a()
                .and(posting.getViewCommentsE());
        send(liberin, new PostingCommentsChangedEvent(posting, viewFilter));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), viewFilter),
                new PostingCommentsUpdatedNotification(posting.getId(), liberin.getTotal()));
    }

}
