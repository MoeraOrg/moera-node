package org.moera.node.global;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.body.Body;
import org.moera.node.config.Config;
import org.moera.node.data.ContactUpgradeRepository;
import org.moera.node.data.DomainUpgrade;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryRevisionUpgrade;
import org.moera.node.data.EntryRevisionUpgradeRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.MediaFileUpgrade;
import org.moera.node.data.MediaFileUpgradeRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.UpgradeType;
import org.moera.node.domain.Domains;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.media.MediaOperations;
import org.moera.node.option.Options;
import org.moera.node.rest.task.upgrade.AllRemoteAvatarsDownloadTask;
import org.moera.node.rest.task.upgrade.AllContactDetailsDownloadTask;
import org.moera.node.rest.task.upgrade.ContactsUpgradeTask;
import org.moera.node.rest.task.upgrade.EncryptAllOptionsJob;
import org.moera.node.task.Jobs;
import org.moera.node.task.JobsManagerInitializedEvent;
import org.moera.node.task.TaskAutowire;
import org.moera.node.userlist.MalwareListOperations;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class Updater {

    private static final Logger log = LoggerFactory.getLogger(Updater.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private Domains domains;

    @Inject
    private Config config;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private EntryRevisionUpgradeRepository entryRevisionUpgradeRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileUpgradeRepository mediaFileUpgradeRepository;

    @Inject
    private ContactUpgradeRepository contactUpgradeRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Jobs jobs;

    @Inject
    private MalwareListOperations malwareListOperations;

    @Inject
    private Transaction tx;

    @EventListener(JobsManagerInitializedEvent.class)
    public void execute() {
        log.info("Executing upgrades");
        executeMediaUpgrades();
        executeDomainUpgrades();
        executeEntryRevisionUpgrades();
        executeContactUpgrades();
    }

    /* Media upgrades */

    private void executeMediaUpgrades() {
        tx.executeRead(this::renamePaddedIds);
        updateMediaFileNames();
        updateMediaFileDigests();
    }

    private void renamePaddedIds() {
        if (mediaFileRepository.countIdWithPadding() > 0) {
            throw new IllegalStateException(
                "Media files with padded IDs were found. Run Moera Node 0.18.0 first to finish the media file"
                    + " ID migration."
            );
        }
    }

    private record MediaFileNameUpgradeResult(
        int count,
        int populatedCount,
        int missingCount,
        int ambiguousCount,
        boolean completed
    ) {
    }

    private void updateMediaFileNames() {
        log.info("Executing media file name upgrades");
        int populatedCount = 0;
        int missingCount = 0;
        int ambiguousCount = 0;

        while (true) {
            log.info(
                "Media file name upgrade so far: {} populated, {} missing, {} ambiguous",
                populatedCount, missingCount, ambiguousCount
            );

            MediaFileNameUpgradeResult result = tx.executeWrite(this::updateMediaFileNamesBatch);
            log.info("Found {} media file name upgrades", result.count());
            if (result.count() == 0) {
                return;
            }

            populatedCount += result.populatedCount();
            missingCount += result.missingCount();
            ambiguousCount += result.ambiguousCount();

            if (result.completed()) {
                break;
            }
        }

        log.info(
            "Media file name upgrade completed: {} populated, {} missing, {} ambiguous",
            populatedCount, missingCount, ambiguousCount
        );
        tx.executeWrite(entryRevisionRepository::clearAttachmentsCache);
    }

    private MediaFileNameUpgradeResult updateMediaFileNamesBatch() {
        List<MediaFileUpgrade> upgrades = mediaFileUpgradeRepository.findPending(
            UpgradeType.MEDIA_FILE_NAME, Pageable.ofSize(PAGE_SIZE)
        );
        int populatedCount = 0;
        int missingCount = 0;
        int ambiguousCount = 0;
        for (MediaFileUpgrade upgrade : upgrades) {
            MediaFile mediaFile = upgrade.getMediaFile();
            List<Path> paths;
            try {
                paths = findMediaFiles(mediaFile);
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Cannot scan local files for media file %s".formatted(mediaFile.getId()), e
                );
            }
            if (paths.size() == 1) {
                mediaFile.setFileName(paths.getFirst().getFileName().toString());
                populatedCount++;
            } else if (paths.isEmpty()) {
                missingCount++;
                log.warn("No local file found for media file {}", mediaFile.getId());
            } else {
                ambiguousCount++;
                log.warn("Several local files found for media file {}", mediaFile.getId());
            }
            mediaFileUpgradeRepository.deleteById(upgrade.getId());
        }

        boolean completed = upgrades.size() < PAGE_SIZE
            || mediaFileUpgradeRepository.findPending(UpgradeType.MEDIA_FILE_NAME, Pageable.ofSize(1)).isEmpty();

        return new MediaFileNameUpgradeResult(
            upgrades.size(), populatedCount, missingCount, ambiguousCount, completed
        );
    }

    private List<Path> findMediaFiles(MediaFile mediaFile) throws IOException {
        List<Path> paths = new ArrayList<>(2);
        Path mediaPath = Path.of(config.getMedia().getPath());
        try (var candidates = Files.newDirectoryStream(mediaPath, mediaFile.getId() + ".*")) {
            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    paths.add(candidate);
                    if (paths.size() == 2) {
                        break;
                    }
                }
            }
        }
        return paths;
    }

    private void updateMediaFileDigests() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        int count;
        do {
            count = tx.executeWrite(() -> {
                List<MediaFile> mediaFiles = mediaFileRepository.findWithNoDigest(pageable);
                mediaFiles.forEach(this::updateDigest);
                return mediaFiles.size();
            });
        } while (count > 0);
    }

    private void updateDigest(MediaFile mediaFile) {
        try {
            mediaFile.setDigest(mediaOperations.digest(mediaFile));
        } catch (IOException e) {
            log.warn("Cannot calculate digest of media file {}: {}", mediaFile.getId(), e.getMessage());
        }
    }

    /* Domain upgrades */

    private void executeDomainUpgrades() {
        downloadAvatars();
        downloadContactDetails();
        encryptOptions();
        autoSubscribeMalwareLists();
    }

    private void autoSubscribeMalwareLists() {
        for (UUID nodeId : findPendingDomainNodeIds(UpgradeType.MALWARE_AUTO_SUBSCRIBE)) {
            tx.executeWrite(() -> {
                universalContext.associate(nodeId);
                malwareListOperations.autoSubscribe();
                domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.MALWARE_AUTO_SUBSCRIBE, nodeId);
            });
        }
    }

    private void downloadAvatars() {
        for (UUID nodeId : findPendingDomainNodeIds(UpgradeType.AVATAR_DOWNLOAD)) {
            var task = new AllRemoteAvatarsDownloadTask();
            taskAutowire.autowireWithoutRequest(task, nodeId);
            taskExecutor.execute(task);
        }
    }

    private void downloadContactDetails() {
        for (UUID nodeId : findPendingDomainNodeIds(UpgradeType.GENDER_DOWNLOAD)) {
            var task = new AllContactDetailsDownloadTask();
            taskAutowire.autowireWithoutRequest(task, nodeId);
            taskExecutor.execute(task);
        }
    }

    private void encryptOptions() {
        if (
            !findPendingDomainNodeIds(UpgradeType.ENCRYPT_OPTIONS).isEmpty()
            && !jobs.isRunning(EncryptAllOptionsJob.class)
        ) {
            jobs.run(EncryptAllOptionsJob.class, new EncryptAllOptionsJob.Parameters());
        }
    }

    private List<UUID> findPendingDomainNodeIds(UpgradeType upgradeType) {
        return tx.executeRead(() ->
            domainUpgradeRepository.findPending(upgradeType).stream()
                .map(DomainUpgrade::getNodeId)
                .toList()
        );
    }

    /* Entry revision upgrades */

    private void executeEntryRevisionUpgrades() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        int count;
        do {
            count = tx.executeWrite(() -> {
                List<EntryRevisionUpgrade> upgrades = entryRevisionUpgradeRepository.findPending(pageable);
                upgrades.forEach(this::process);
                return upgrades.size();
            });
        } while (count > 0);
    }

    private void process(EntryRevisionUpgrade upgrade) {
        switch (upgrade.getUpgradeType()) {
            case UPDATE_SIGNATURE:
                updateSignature(upgrade.getEntryRevision());
                break;
            case JSON_BODY:
                convertBodyToJson(upgrade.getEntryRevision());
                break;
            case UPDATE_DIGEST:
                updateDigest(upgrade.getEntryRevision());
                break;
            default:
                break;
        }
        entryRevisionUpgradeRepository.delete(upgrade);
    }

    private void updateSignature(EntryRevision revision) {
        UUID nodeId = revision.getEntry().getNodeId();
        Options options = domains.getDomainOptions(nodeId);
        if (options == null) {
            log.error("No domain exists for node {}", nodeId);
            return;
        }
        if (ObjectUtils.isEmpty(options.nodeName())) {
            log.info("No name registered for node {}", nodeId);
            return;
        }
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            log.info("No signing key found for node {}", nodeId);
            return;
        }
        Posting posting = (Posting) revision.getEntry();
        byte[] fingerprint = PostingFingerprintBuilder.build(posting, revision);
        revision.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        revision.setSignatureVersion(PostingFingerprintBuilder.LATEST_VERSION);
        log.info("Signature upgraded for entry {}, revision {}", posting.getId(), revision.getId());
    }

    private void convertBodyToJson(EntryRevision revision) {
        Body body = new Body();
        body.setText(revision.getBody());
        revision.setBody(body.getEncoded());
        body.setText(revision.getBodyPreview());
        revision.setBodyPreview(body.getEncoded());
        body.setText(revision.getBodySrc());
        revision.setBodySrc(body.getEncoded());
        log.info("Body of entry {}, revision {} converted to JSON", revision.getEntry().getId(), revision.getId());
    }

    private void updateDigest(EntryRevision revision) {
        Posting posting = (Posting) revision.getEntry();
        byte[] fingerprint = PostingFingerprintBuilder.build(posting, revision);
        revision.setDigest(CryptoUtil.digest(fingerprint));
        log.info("Digest upgraded for entry {}, revision {}", posting.getId(), revision.getId());
    }

    /* Contact upgrades */

    private void executeContactUpgrades() {
        downloadProfiles();
    }

    private void downloadProfiles() {
        if (tx.executeRead(() -> contactUpgradeRepository.countPending(UpgradeType.PROFILE_DOWNLOAD)) > 0) {
            var task = new ContactsUpgradeTask();
            taskAutowire.autowireWithoutRequestAndDomain(task);
            taskExecutor.execute(task);
        }
    }

}
