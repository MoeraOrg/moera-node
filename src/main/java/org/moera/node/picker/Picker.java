package org.moera.node.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.WhoAmI;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySource;
import org.moera.node.data.EntrySourceRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class Picker extends Task {

    private static final Logger log = LoggerFactory.getLogger(Picker.class);

    private final String remoteNodeName;
    private String remoteFullName;
    private MediaFile remoteAvatarMediaFile;
    private String remoteAvatarShape;
    private final BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private final PickerPool pool;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private EntrySourceRepository entrySourceRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private MediaManager mediaManager;

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
    protected void execute() {
        try {
            fetchNodeDetails();
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
                    try {
                        download(pick);
                    } catch (Throwable e) {
                        failed(pick, e);
                        throw e;
                    }
                }
            }
        } catch (Throwable e) {
            error(e);
        } finally {
            pool.deletePicker(nodeId, remoteNodeName);
        }
    }

    private void fetchNodeDetails() throws MoeraNodeException {
        WhoAmI remote = nodeApi.at(remoteNodeName).whoAmI();
        remoteFullName = remote.getFullName();
        remoteAvatarMediaFile = mediaManager.downloadPublicMedia(remoteNodeName, remote.getAvatar());
        remoteAvatarShape = remote.getAvatar() != null ? remote.getAvatar().getShape() : null;
    }

    private void download(Pick pick) throws Exception {
        log.info(
            "Downloading pick {} from node {}, postingId = {}",
            LogUtil.format(pick.getId()), LogUtil.format(remoteNodeName), LogUtil.format(pick.getRemotePostingId())
        );

        List<Liberin> liberins = new ArrayList<>();
        List<Pick> picks = new ArrayList<>();
        Posting posting = tx.executeWriteWithExceptions(() -> {
            Posting p = downloadPosting(
                pick.getRemotePostingId(),
                pick.getFeedName(),
                pick.getMediaFileOwner(),
                pick.isRecommended(),
                liberins,
                picks
            );
            saveSources(p, pick);
            return p;
        });
        liberins.forEach(this::send);
        picks.forEach(pool::pick);

        succeeded(posting, pick);
    }

    private Posting downloadPosting(
        String remotePostingId,
        String feedName,
        MediaFileOwner parentMedia,
        boolean recommended,
        List<Liberin> liberins,
        List<Pick> picks
    ) throws MoeraNodeException {
        PostingInfo postingInfo = nodeApi
            .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
            .getPosting(remotePostingId, false);
        MediaFile ownerAvatar = mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getOwnerAvatar());
        boolean original = PostingInfoUtil.isOriginal(postingInfo);
        String receiverName = original ? remoteNodeName : postingInfo.getReceiverName();
        String receiverFullName = original ? postingInfo.getOwnerFullName() : postingInfo.getReceiverFullName();
        String receiverGender = original ? postingInfo.getOwnerGender() : postingInfo.getReceiverGender();
        MediaFile receiverAvatar = original
            ? ownerAvatar
            : mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getReceiverAvatar());
        String receiverAvatarShape;
        if (original) {
            receiverAvatarShape = postingInfo.getOwnerAvatar() != null
                ? postingInfo.getOwnerAvatar().getShape()
                : null;
        } else {
            receiverAvatarShape = postingInfo.getReceiverAvatar() != null
                ? postingInfo.getReceiverAvatar().getShape()
                : null;
        }
        String receiverPostingId = original ? remotePostingId : postingInfo.getReceiverPostingId();
        Posting posting = postingRepository.findByReceiverId(nodeId, receiverName, receiverPostingId).orElse(null);
        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setParentMedia(parentMedia);
            posting.setReceiverName(receiverName);
            posting.setReceiverFullName(receiverFullName);
            posting.setReceiverGender(receiverGender);
            posting.setReceiverAvatarMediaFile(receiverAvatar);
            posting.setReceiverAvatarShape(receiverAvatarShape);
            posting.setReceiverGender(receiverGender);
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            posting.setRecommended(recommended);
            posting = postingRepository.save(posting);
            PostingInfoUtil.toPickedPosting(postingInfo, posting);
            createRevision(posting, postingInfo);
            downloadMedia(postingInfo, null, posting.getCurrentRevision(), picks);
            updateRevision(posting, postingInfo, posting.getCurrentRevision());
            universalContext.subscriptionsUpdated();
            liberins.add(new PostingAddedLiberin(posting));
            publish(feedName, posting, liberins);
        } else if (!postingInfo.getEditedAt().equals(Util.toEpochSecond(posting.getEditedAt()))) {
            Principal latestView = posting.getViewE();
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            PostingInfoUtil.toPickedPosting(postingInfo, posting);
            EntryRevision latest = posting.getCurrentRevision();
            createRevision(posting, postingInfo);
            downloadMedia(postingInfo, posting.getId(), posting.getCurrentRevision(), picks);
            updateRevision(posting, postingInfo, posting.getCurrentRevision());
            if (posting.getDeletedAt() == null) {
                liberins.add(new PostingUpdatedLiberin(posting, latest, latestView));
            } else {
                posting.setDeletedAt(null);
                publish(feedName, posting, liberins);
                liberins.add(new PostingRestoredLiberin(posting));
            }
        }
        posting = postingRepository.saveAndFlush(posting);
        mediaOperations.updatePermissions(posting);

        var reactionTotals = reactionTotalRepository.findAllByEntryId(posting.getId());
        if (!reactionTotalOperations.isSame(reactionTotals, postingInfo.getReactions())) {
            reactionTotalOperations.replaceAll(posting, postingInfo.getReactions());
        }

        return posting;
    }

    private void createRevision(Posting posting, PostingInfo postingInfo) {
        if (postingInfo.getRevisionId().equals(posting.getCurrentReceiverRevisionId())) {
            return;
        }

        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision = entryRevisionRepository.save(revision);
        posting.addRevision(revision);
        posting.setTotalRevisions(posting.getTotalRevisions() + 1);

        if (posting.getCurrentRevision() != null) {
            posting.getCurrentRevision().setDeletedAt(Util.now());
            if (posting.getCurrentRevision().getReceiverDeletedAt() == null) {
                posting.getCurrentRevision().setReceiverDeletedAt(Util.toTimestamp(postingInfo.getRevisionCreatedAt()));
            }
        }
        posting.setCurrentRevision(revision);
        posting.setCurrentReceiverRevisionId(revision.getReceiverRevisionId());
    }

    private void updateRevision(Posting posting, PostingInfo postingInfo, EntryRevision revision) {
        PostingInfoUtil.toPickedEntryRevision(postingInfo, revision);

        byte[] fingerprint = PostingFingerprintBuilder.build(revision.getSignatureVersion(), posting, revision);
        revision.setDigest(CryptoUtil.digest(fingerprint));
    }

    private void downloadMedia(
        PostingInfo postingInfo, UUID entryId, EntryRevision revision, List<Pick> picks
    ) throws MoeraNodeException {
        int ordinal = 0;
        for (MediaAttachment attach : postingInfo.getMedia()) {
            MediaFileOwner media = mediaManager.downloadPrivateMedia(
                remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_MEDIA), attach.getMedia(), entryId
            );
            if (media != null) {
                EntryAttachment attachment = new EntryAttachment(revision, media, ordinal++);
                attachment.setEmbedded(attach.isEmbedded());
                attachment.setRemoteMediaId(attach.getMedia().getId());
                attachment = entryAttachmentRepository.save(attachment);
                revision.addAttachment(attachment);

                if (attach.getMedia().getPostingId() != null) {
                    picks.add(pickMediaPosting(media, attach.getMedia().getPostingId()));
                }
            }
        }
    }

    private Pick pickMediaPosting(MediaFileOwner media, String remotePostingId) {
        Pick pick = new Pick();
        pick.setRemoteNodeName(remoteNodeName);
        pick.setRemotePostingId(remotePostingId);
        pick.setMediaFileOwner(media);
        return pick;
    }

    private void publish(String feedName, Posting posting, List<Liberin> liberins) {
        if (feedName == null) {
            return;
        }
        int totalStories = storyRepository.countByFeedAndTypeAndEntryId(
            nodeId, feedName, StoryType.POSTING_ADDED, posting.getId()
        );
        if (totalStories > 0) {
            return;
        }
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName(feedName);
        storyOperations.publish(posting, Collections.singletonList(publication), nodeId, liberins::add);
    }

    private void saveSources(Posting posting, Pick pick) {
        if (ObjectUtils.isEmpty(pick.getRemoteFeedName())) {
            return;
        }
        List<EntrySource> sources = entrySourceRepository.findAllByEntryId(posting.getId());
        if (sources.stream().anyMatch(pick::isSame)) {
            return;
        }
        EntrySource entrySource = new EntrySource();
        entrySource.setId(UUID.randomUUID());
        entrySource.setEntry(posting);
        entrySource.setRemoteFullName(remoteFullName);
        entrySource.setRemoteAvatarMediaFile(remoteAvatarMediaFile);
        entrySource.setRemoteAvatarShape(remoteAvatarShape);
        pick.toEntrySource(entrySource);
        entrySourceRepository.save(entrySource);
    }

    private void succeeded(Posting posting, Pick pick) {
        log.info("Posting downloaded successfully, id = {}", posting.getId());
        pool.pickSucceeded(pick);
    }

    private void error(Throwable e) {
        log.error(e.getMessage());
        log.debug("Error picking a post", e);
    }

    private void failed(Pick pick, Throwable e) {
        boolean fatal = e instanceof MoeraNodeApiNotFoundException;
        pool.pickFailed(pick, fatal);
    }

}
