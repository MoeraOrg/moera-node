package org.moera.node.picker;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
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
    private PlatformTransactionManager txManager;

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
            PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, pick.getRemotePostingId());
            Posting posting = postingRepository.findByReceiverId(nodeId, remoteNodeName, pick.getRemotePostingId())
                    .orElse(null);
            if (posting == null) {
                posting = new Posting();
                posting.setId(UUID.randomUUID());
                posting.setNodeId(nodeId);
                posting.setReceiverName(remoteNodeName);
                posting = postingRepository.save(posting);
            }
            postingInfo.toPickedPosting(posting);

            PostingRevisionInfo[] revisionInfos = nodeApi.getPostingRevisions(remoteNodeName, pick.getRemotePostingId());
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

            commitTransaction(status);

            succeeded(posting);
        } catch (Exception e) {
            rollbackTransaction(status);
            error(e);
        }
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
