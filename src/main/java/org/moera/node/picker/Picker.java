package org.moera.node.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.event.EventManager;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.event.Event;
import org.moera.node.rest.StoryOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class Picker extends Task {

    private static Logger log = LoggerFactory.getLogger(Picker.class);

    private String remoteNodeName;
    private BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private PickerPool pool;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private EventManager eventManager;

    public Picker(PickerPool pool, String remoteNodeName) {
        this.pool = pool;
        this.remoteNodeName = remoteNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Pick pick) throws InterruptedException {
        queue.put(pick);
    }

    @Override
    public void run() {
        while (!stopped) {
            Pick pick;
            try {
                pick = queue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                continue;
            }
            if (pick == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                download(pick);
            }
        }
        pool.deletePicker(nodeId, remoteNodeName);
    }

    private void download(Pick pick) {
        initLoggingDomain();
        log.info("Downloading from node '{}', postingId = {}", remoteNodeName, pick.getRemotePostingId());

        nodeApi.setNodeId(nodeId);
        TransactionStatus status = beginTransaction();
        try {
            Posting posting = downloadPosting(pick.getRemotePostingId());
            downloadRevisions(posting);
            List<Event> events = new ArrayList<>();
            publish(pick.getFeedName(), posting, events);

            commitTransaction(status);
            events.forEach(event -> eventManager.send(nodeId, event));
            succeeded(posting);
        } catch (Exception e) {
            rollbackTransaction(status);
            error(e);
        }
    }

    private Posting downloadPosting(String remotePostingId) throws NodeApiException {
        PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, remotePostingId);
        Posting posting = postingRepository.findByReceiverId(nodeId, remoteNodeName, remotePostingId)
                .orElse(null);
        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setReceiverName(remoteNodeName);
            posting = postingRepository.save(posting);
        }
        postingInfo.toPickedPosting(posting);

        return posting;
    }

    private void downloadRevisions(Posting posting) throws NodeApiException {
        PostingRevisionInfo[] revisionInfos = nodeApi.getPostingRevisions(remoteNodeName, posting.getReceiverEntryId());
        EntryRevision revision = null;
        for (PostingRevisionInfo revisionInfo : revisionInfos) {
            revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(posting);
            revision = entryRevisionRepository.save(revision);
            posting.addRevision(revision);
            revisionInfo.toPickedEntryRevision(revision);
            PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
            revision.setDigest(CryptoUtil.digest(fingerprint));
        }
        posting.setTotalRevisions(revisionInfos.length);
        posting.setCurrentRevision(revision);
        if (revision != null) {
            posting.setCurrentReceiverRevisionId(revision.getReceiverRevisionId());
        }
    }

    private void publish(String feedName, Posting posting, List<Event> events) {
        int totalStories = storyRepository.countByFeedAndTypeAndEntryId(nodeId, feedName, StoryType.POSTING_ADDED,
                posting.getId());
        if (totalStories > 0) {
            return;
        }
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName(feedName);
        storyOperations.publish(posting, Collections.singletonList(publication), nodeId, events::add);
    }

    private void succeeded(Posting posting) {
        initLoggingDomain();
        log.info("Posting downloaded successfully, id = {}", posting.getId());
    }

    private void error(Throwable e) {
        failed(e.getMessage());
    }

    private void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

    private TransactionStatus beginTransaction() {
        return txManager != null ? txManager.getTransaction(new DefaultTransactionDefinition()) : null;
    }

    private void commitTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.commit(status);
        }
    }

    private void rollbackTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.rollback(status);
        }
    }

}
