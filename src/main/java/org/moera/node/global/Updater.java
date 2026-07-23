package org.moera.node.global;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
import org.moera.node.rest.task.upgrade.MediaFileRenamePaddedIdsJob;
import org.moera.node.task.Jobs;
import org.moera.node.task.JobsManagerInitializedEvent;
import org.moera.node.task.TaskAutowire;
import org.moera.node.userlist.MalwareListOperations;
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

    @EventListener(JobsManagerInitializedEvent.class)
    @Transactional
    public void execute() {
        executeMediaUpgrades();
        executeDomainUpgrades();
        executeEntryRevisionUpgrades();
        executeContactUpgrades();
    }

    private void executeDomainUpgrades() {
        downloadAvatars();
        downloadContactDetails();
        encryptOptions();
        autoSubscribeMalwareLists();
    }

    private void autoSubscribeMalwareLists() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.MALWARE_AUTO_SUBSCRIBE);
        for (DomainUpgrade upgrade : upgrades) {
            universalContext.associate(upgrade.getNodeId());
            malwareListOperations.autoSubscribe();
            domainUpgradeRepository.delete(upgrade);
        }
    }

    private void downloadAvatars() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.AVATAR_DOWNLOAD);
        for (DomainUpgrade upgrade : upgrades) {
            var task = new AllRemoteAvatarsDownloadTask();
            taskAutowire.autowireWithoutRequest(task, upgrade.getNodeId());
            taskExecutor.execute(task);
        }
    }

    private void downloadContactDetails() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.GENDER_DOWNLOAD);
        for (DomainUpgrade upgrade : upgrades) {
            var task = new AllContactDetailsDownloadTask();
            taskAutowire.autowireWithoutRequest(task, upgrade.getNodeId());
            taskExecutor.execute(task);
        }
    }

    private void encryptOptions() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.ENCRYPT_OPTIONS);
        if (!upgrades.isEmpty() && !jobs.isRunning(EncryptAllOptionsJob.class)) {
            jobs.run(EncryptAllOptionsJob.class, new EncryptAllOptionsJob.Parameters());
        }
    }

    private void executeEntryRevisionUpgrades() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        List<EntryRevisionUpgrade> upgrades;
        do {
            upgrades = entryRevisionUpgradeRepository.findPending(pageable);
            upgrades.forEach(this::process);
        } while (!upgrades.isEmpty());
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

    private void executeMediaUpgrades() {
        updateMediaFileNames();
        updateMediaFileDigests();
        renamePaddedIds();
    }

    private void updateMediaFileNames() {
        List<MediaFileUpgrade> firstBatch = mediaFileUpgradeRepository.findPending(
            UpgradeType.MEDIA_FILE_NAME, Pageable.ofSize(PAGE_SIZE)
        );
        if (firstBatch.isEmpty()) {
            return;
        }

        int populatedCount = 0;
        int missingCount = 0;
        int ambiguousCount = 0;
        List<MediaFileUpgrade> upgrades = firstBatch;
        do {
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
            upgrades = mediaFileUpgradeRepository.findPending(
                UpgradeType.MEDIA_FILE_NAME, Pageable.ofSize(PAGE_SIZE)
            );
        } while (!upgrades.isEmpty());

        log.info(
            "Media file name upgrade completed: {} populated, {} missing, {} ambiguous",
            populatedCount, missingCount, ambiguousCount
        );
        entryRevisionRepository.clearAttachmentsCache();
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
        List<MediaFile> mediaFiles;
        do {
            mediaFiles = mediaFileRepository.findWithNoDigest(pageable);
            mediaFiles.forEach(this::updateDigest);
        } while (!mediaFiles.isEmpty());
    }

    private void updateDigest(MediaFile mediaFile) {
        try {
            mediaFile.setDigest(mediaOperations.digest(mediaFile));
        } catch (IOException e) {
            log.warn("Cannot calculate digest of media file {}: {}", mediaFile.getId(), e.getMessage());
        }
    }

    private void renamePaddedIds() {
        if (mediaFileRepository.countIdWithPadding() > 0 && !jobs.isRunning(MediaFileRenamePaddedIdsJob.class)) {
            jobs.run(MediaFileRenamePaddedIdsJob.class, new MediaFileRenamePaddedIdsJob.Parameters());
        }
    }

    private void executeContactUpgrades() {
        downloadProfiles();
    }

    private void downloadProfiles() {
        if (contactUpgradeRepository.countPending(UpgradeType.PROFILE_DOWNLOAD) > 0) {
            var task = new ContactsUpgradeTask();
            taskAutowire.autowireWithoutRequestAndDomain(task);
            taskExecutor.execute(task);
        }
    }

}
