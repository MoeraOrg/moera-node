package org.moera.node.picker;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.jetbrains.annotations.NotNull;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaLeaseAttributes;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.WhoAmI;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.api.node.MoeraNodeConcurrencyException;
import org.moera.node.data.Entry;
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
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.global.ServeContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.media.MediaManager.PreparedPrivateMedia;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.RemoteMediaOperations;
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
    private String remoteSourceUri;
    private MediaFile remoteAvatarMediaFile;
    private String remoteAvatarShape;
    private final BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private final PickerPool pool;

    @Inject
    private DirectServeOperations directServeOperations;

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
    private RemoteMediaOperations remoteMediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private MediaManager mediaManager;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    private record MediaKey(String nodeName, String mediaId) {
    }

    private record PostingSnapshot(UUID postingId, UUID revisionId, Long editedAt, Set<MediaKey> attachments) {
    }

    private record PreparedAttachment(
        MediaAttachment attachment, int ordinal, MediaKey key, PrivateMediaFileInfo info,
        String leaseId, PreparedPrivateMedia media
    ) {
    }

    public Picker(PickerPool pool, String remoteNodeName) {
        this.pool = pool;
        this.remoteNodeName = remoteNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Pick pick) {
        queue.add(pick);
    }

    @Override
    protected void execute() {
        try {
            fetchNodeDetails();
            while (!stopped) {
                Pick pick;
                try {
                    pick = queue.poll(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
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
        remoteSourceUri = remote.getSourceUri();
        remoteAvatarMediaFile = mediaManager.downloadPublicMedia(remoteNodeName, remote.getAvatar());
        remoteAvatarShape = remote.getAvatar() != null ? remote.getAvatar().getShape() : null;
    }

    private void download(Pick pick) throws Exception {
        log.info(
            "Downloading pick {} from node {}, postingId = {}",
            LogUtil.format(pick.getId()), LogUtil.format(remoteNodeName), LogUtil.format(pick.getRemotePostingId())
        );

        PostingInfo postingInfo = nodeApi
            .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
            .getPosting(pick.getRemotePostingId(), false);
        MediaFile ownerAvatar = mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getOwnerAvatar());
        MediaFile receiverAvatar = PostingInfoUtil.isOriginal(postingInfo)
            ? ownerAvatar
            : mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getReceiverAvatar());
        PostingSnapshot snapshot = tx.executeRead(() -> snapshot(postingInfo, pick.getRemotePostingId()));
        List<PreparedAttachment> attachments =
            snapshot.postingId() == null || !postingInfo.getEditedAt().equals(snapshot.editedAt())
                ? prepareMedia(postingInfo, snapshot)
                : List.of();

        List<Liberin> liberins = new ArrayList<>();
        List<Pick> picks = new ArrayList<>();
        Posting posting = tx.executeWriteWithExceptions(() -> {
            Posting p = savePosting(
                postingInfo,
                ownerAvatar,
                receiverAvatar,
                snapshot,
                attachments,
                pick.getRemotePostingId(),
                pick.getFeedName(),
                pick.getMediaFileOwner(),
                pick.getRemoteMediaFile(),
                pick.getParentMediaEntry(),
                pick.isRecommended(),
                pick.isViewed(),
                pick.getPublishAt(),
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

    private PostingSnapshot snapshot(PostingInfo info, String remotePostingId) {
        boolean original = PostingInfoUtil.isOriginal(info);
        Posting posting = postingRepository.findByReceiverId(
            nodeId,
            original ? remoteNodeName : info.getReceiverName(),
            original ? remotePostingId : info.getReceiverPostingId()
        ).orElse(null);
        if (posting == null) {
            return new PostingSnapshot(null, null, null, Set.of());
        }

        Set<MediaKey> attachments = new HashSet<>();
        EntryRevision revision = posting.getCurrentRevision();
        if (revision != null) {
            for (EntryAttachment attachment : revision.getAttachments()) {
                RemoteMediaFile remoteMedia = attachment.getRemoteMediaFile();
                if (remoteMedia != null) {
                    attachments.add(new MediaKey(remoteMedia.getNodeName(), remoteMedia.getMediaId()));
                }
            }
        }

        return new PostingSnapshot(
            posting.getId(),
            revision != null ? revision.getId() : null,
            Util.toEpochSecond(posting.getEditedAt()),
            attachments
        );
    }

    private Posting savePosting(
        PostingInfo postingInfo,
        MediaFile ownerAvatar,
        MediaFile receiverAvatar,
        PostingSnapshot snapshot,
        List<PreparedAttachment> attachments,
        String remotePostingId,
        String feedName,
        MediaFileOwner parentMedia,
        RemoteMediaFile parentRemoteMedia,
        Entry parentMediaEntry,
        boolean recommended,
        boolean viewed,
        Timestamp publishAt,
        List<Liberin> liberins,
        List<Pick> picks
    ) throws MoeraNodeException {
        boolean original = PostingInfoUtil.isOriginal(postingInfo);
        String receiverName = original ? remoteNodeName : postingInfo.getReceiverName();
        String receiverFullName = original ? postingInfo.getOwnerFullName() : postingInfo.getReceiverFullName();
        String receiverSourceUri = original ? postingInfo.getOwnerSourceUri() : postingInfo.getReceiverSourceUri();
        String receiverGender = original ? postingInfo.getOwnerGender() : postingInfo.getReceiverGender();
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
        Posting posting = postingRepository.findByReceiverIdForUpdate(nodeId, receiverName, receiverPostingId)
            .orElse(null);
        UUID postingId = posting != null ? posting.getId() : null;
        UUID revisionId = posting != null && posting.getCurrentRevision() != null
            ? posting.getCurrentRevision().getId()
            : null;
        Long editedAt = posting != null ? Util.toEpochSecond(posting.getEditedAt()) : null;

        if (
            !Objects.equals(snapshot.postingId(), postingId)
            || !Objects.equals(snapshot.revisionId(), revisionId)
            || !Objects.equals(snapshot.editedAt(), editedAt)
        ) {
            throw new MoeraNodeConcurrencyException("Posting changed while downloading media, retry the pick");
        }

        ownerAvatar = ownerAvatar != null ? entityManager.find(MediaFile.class, ownerAvatar.getId()) : null;
        receiverAvatar = receiverAvatar != null ? entityManager.find(MediaFile.class, receiverAvatar.getId()) : null;

        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setParentMedia(
                parentMedia != null ? requireEntity(MediaFileOwner.class, parentMedia.getId()) : null
            );
            posting.setParentRemoteMedia(
                parentRemoteMedia != null ? requireEntity(RemoteMediaFile.class, parentRemoteMedia.getId()) : null
            );
            posting.setParentMediaEntry(
                parentMediaEntry != null ? requireEntity(Entry.class, parentMediaEntry.getId()) : null
            );
            posting.setReceiverName(receiverName);
            posting.setReceiverFullName(receiverFullName);
            posting.setReceiverSourceUri(receiverSourceUri);
            posting.setReceiverGender(receiverGender);
            posting.setReceiverAvatarMediaFile(receiverAvatar);
            posting.setReceiverAvatarShape(receiverAvatarShape);
            posting.setReceiverGender(receiverGender);
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            posting.setRecommended(recommended);
            posting = postingRepository.save(posting);
            PostingInfoUtil.toPickedPosting(postingInfo, posting);
            createRevision(posting, postingInfo);
            attachMedia(attachments, null, posting.getCurrentRevision(), null, picks);
            updateRevision(posting, postingInfo, posting.getCurrentRevision());
            universalContext.subscriptionsUpdated();
            liberins.add(new PostingAddedLiberin(posting));
            publish(feedName, posting, viewed, publishAt, liberins);
        } else if (!postingInfo.getEditedAt().equals(Util.toEpochSecond(posting.getEditedAt()))) {
            Principal latestView = posting.getViewE();
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            PostingInfoUtil.toPickedPosting(postingInfo, posting);
            EntryRevision latest = posting.getCurrentRevision();
            createRevision(posting, postingInfo);
            attachMedia(attachments, posting.getId(), posting.getCurrentRevision(), latest, picks);
            updateRevision(posting, postingInfo, posting.getCurrentRevision());
            if (posting.getDeletedAt() == null) {
                liberins.add(new PostingUpdatedLiberin(posting, latest, latestView));
            } else {
                posting.setDeletedAt(null);
                publish(feedName, posting, viewed, publishAt, liberins);
                liberins.add(new PostingRestoredLiberin(posting));
            }
        } else {
            boolean published = storyRepository.countByFeedAndTypeAndEntryId(
                nodeId, feedName, StoryType.POSTING_ADDED, posting.getId()
            ) > 0;
            if (!published) {
                publish(feedName, posting, viewed, publishAt, liberins);
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

    private <T> T requireEntity(Class<T> type, UUID id) throws MoeraNodeException {
        T entity = entityManager.find(type, id);
        if (entity == null) {
            throw new MoeraNodeConcurrencyException(
                "Parent media or entry disappeared while downloading media, retry the pick"
            );
        }
        return entity;
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
        PostingInfoUtil.toPickedEntryRevision(
            postingInfo, revision, new ServeContext(directServeOperations, getOptions())
        );

        byte[] fingerprint = PostingFingerprintBuilder.build(revision.getSignatureVersion(), posting, revision);
        revision.setDigest(CryptoUtil.digest(fingerprint));
    }

    private List<PreparedAttachment> prepareMedia(PostingInfo postingInfo, PostingSnapshot snapshot)
        throws MoeraNodeException {
        List<PreparedAttachment> prepared = new ArrayList<>();
        int ordinal = 0;
        for (MediaAttachment attach : postingInfo.getMedia()) {
            PreparedAttachment attachment = prepareMedia(attach, postingInfo.getId(), ordinal++, snapshot);
            if (attachment != null) {
                prepared.add(attachment);
            }
        }
        return prepared;
    }

    private PreparedAttachment prepareMedia(
        MediaAttachment attach, String remotePostingId, int ordinal, PostingSnapshot snapshot
    ) throws MoeraNodeException {
        if (attach.getMedia() == null && attach.getRemoteMedia() == null) {
            log.warn(
                "Attachment of the posting {} at node {} does not contain a media",
                remotePostingId, remoteNodeName
            );
            return null;
        }

        String mediaNodeName = attach.getMedia() != null ? remoteNodeName : attach.getRemoteMedia().getNodeName();
        String mediaId = attach.getMedia() != null ? attach.getMedia().getId() : attach.getRemoteMedia().getMediaId();

        if (ObjectUtils.isEmpty(mediaNodeName) || ObjectUtils.isEmpty(mediaId)) {
            log.warn(
                "Attachment of the posting {} at node {} does not contain a media",
                remotePostingId, remoteNodeName
            );
            return null;
        }

        MediaKey key = new MediaKey(mediaNodeName, mediaId);
        if (snapshot.attachments().contains(key)) {
            return new PreparedAttachment(attach, ordinal, key, null, null, null);
        }
        PrivateMediaFileInfo mediaInfo = attach.getMedia();
        if (mediaInfo == null) {
            mediaInfo = nodeApi.at(mediaNodeName, generateCarte(mediaNodeName, Scope.VIEW_CONTENT))
                .getPrivateMediaInfo(mediaId, attach.getRemoteMedia().getGrant());
        }
        String leaseId = leaseMedia(mediaNodeName, mediaId, mediaInfo.getSize(), remotePostingId);
        PreparedPrivateMedia media = leaseId == null
            ? mediaManager.preparePrivateMedia(
                mediaNodeName,
                generateCarte(mediaNodeName, Scope.VIEW_CONTENT),
                mediaInfo,
                Math.min(
                    universalContext.getOptions().getInt("media.max-size"),
                    universalContext.getOptions().getInt("posting.media.max-size")
                ),
                snapshot.postingId()
            )
            : null;
        return new PreparedAttachment(attach, ordinal, key, mediaInfo, leaseId, media);
    }

    private void attachMedia(
        List<PreparedAttachment> attachments, UUID entryId, EntryRevision revision,
        EntryRevision prevRevision, List<Pick> picks
    ) throws MoeraNodeException {
        for (PreparedAttachment attachment : attachments) {
            attachMedia(attachment, entryId, revision, prevRevision, picks);
        }
    }

    private void attachMedia(
        PreparedAttachment prepared, UUID entryId, EntryRevision revision, EntryRevision prevRevision, List<Pick> picks
    ) throws MoeraNodeException {
        MediaAttachment attach = prepared.attachment();
        var existing = prevRevision != null
            ? prevRevision.getAttachments().stream()
                .filter(ea -> ea.getRemoteMediaFile() != null)
                .filter(ea ->
                    Objects.equals(ea.getRemoteMediaFile().getNodeName(), prepared.key().nodeName())
                    && Objects.equals(ea.getRemoteMediaFile().getMediaId(), prepared.key().mediaId())
                )
                .findFirst()
                .orElse(null)
            : null;

        MediaFileOwner media;
        RemoteMediaFile remoteMedia;

        if (existing == null) {
            if (prepared.info() == null) {
                throw new MoeraNodeConcurrencyException("Attachment changed while downloading media, retry the pick");
            }
            media = mediaManager.ownPreparedPrivateMedia(prepared.media(), entryId);
            remoteMedia = remoteMediaOperations.store(prepared.key().nodeName(), prepared.info(), prepared.leaseId());
        } else {
            media = existing.getMediaFileOwner();
            remoteMedia = existing.getRemoteMediaFile();
        }

        EntryAttachment attachment = new EntryAttachment(revision, media, remoteMedia, prepared.ordinal());
        attachment.setEmbedded(attach.isEmbedded());
        attachment = entryAttachmentRepository.save(attachment);
        revision.addAttachment(attachment);

        if (attach.getPostingId() != null) {
            picks.add(pickMediaPosting(media, remoteMedia, revision.getEntry(), attach.getPostingId()));
        }
    }

    private Pick pickMediaPosting(
        MediaFileOwner media,
        RemoteMediaFile remoteMedia,
        Entry parentMediaEntry,
        String remotePostingId
    ) {
        Pick pick = new Pick();
        pick.setRemoteNodeName(remoteNodeName);
        pick.setRemotePostingId(remotePostingId);
        pick.setMediaFileOwner(media);
        pick.setRemoteMediaFile(remoteMedia);
        pick.setParentMediaEntry(parentMediaEntry);
        return pick;
    }

    private String leaseMedia(String mediaNodeName, String mediaId, long mediaSize, String remotePostingId) {
        long maxSize = universalContext.getOptions().getLong("posting.media.max-size");
        if (mediaSize <= maxSize) {
            return null;
        }

        var attrs = new MediaLeaseAttributes();
        attrs.setNodeName(universalContext.nodeName());
        attrs.setMediaId(mediaId);
        attrs.setPostingId(remotePostingId);
        try {
            var lease = nodeApi
                .at(mediaNodeName, generateCarte(mediaNodeName, Scope.VIEW_CONTENT))
                .createMediaLease(attrs);
            return lease.getId();
        } catch (MoeraNodeException e) {
            log.warn(
                "Failed to lease media {} from posting {} at node {}: {}",
                mediaId, remotePostingId, mediaNodeName, e.getMessage()
            );
            return null;
        }
    }

    private void publish(
        String feedName, Posting posting, boolean viewed, Timestamp publishAt, List<Liberin> liberins
    ) {
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
        publication.setViewed(viewed);
        publication.setPublishAt(Util.toEpochSecond(publishAt));
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
        entrySource.setRemoteSourceUri(remoteSourceUri);
        entrySource.setRemoteAvatarMediaFile(
            remoteAvatarMediaFile != null ? entityManager.find(MediaFile.class, remoteAvatarMediaFile.getId()) : null
        );
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
