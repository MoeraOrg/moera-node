package org.moera.node.global;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.DomainUpgrade;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionUpgrade;
import org.moera.node.data.EntryRevisionUpgradeRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.UpgradeType;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.body.Body;
import org.moera.node.operations.PostingOperations;
import org.moera.node.option.Options;
import org.moera.node.rest.task.AllRemoteAvatarsDownloadTask;
import org.moera.node.rest.task.AllRemoteProfilesSubscriptionTask;
import org.moera.node.task.TaskAutowire;
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
    private UniversalContext universalContext;

    @Inject
    private EntryRevisionUpgradeRepository entryRevisionUpgradeRepository;

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void execute() {
        executeDomainUpgrades();
        executeEntryRevisionUpgrades();
        executeMediaUpgrades();
    }

    private void executeDomainUpgrades() {
        subscribeToProfiles();
        downloadAvatars();
    }

    private void subscribeToProfiles() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.PROFILE_SUBSCRIBE);
        for (DomainUpgrade upgrade : upgrades) {
            var task = new AllRemoteProfilesSubscriptionTask();
            taskAutowire.autowireWithoutRequest(task, upgrade.getNodeId());
            taskExecutor.execute(task);
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

    private void executeEntryRevisionUpgrades() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        List<EntryRevisionUpgrade> upgrades;
        do {
            upgrades = entryRevisionUpgradeRepository.findPending(pageable);
            upgrades.forEach(this::process);
        } while (upgrades.size() > 0);
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
        PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
        revision.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        revision.setSignatureVersion(PostingFingerprint.VERSION);
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
        PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
        revision.setDigest(CryptoUtil.digest(fingerprint));
        log.info("Digest upgraded for entry {}, revision {}", posting.getId(), revision.getId());
    }

    private void executeMediaUpgrades() {
        updateMediaFileDigests();
        createMediaPostings();
    }

    private void updateMediaFileDigests() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        List<MediaFile> mediaFiles;
        do {
            mediaFiles = mediaFileRepository.findWithNoDigest(pageable);
            mediaFiles.forEach(this::updateDigest);
        } while (mediaFiles.size() > 0);
    }

    private void updateDigest(MediaFile mediaFile) {
        try {
            mediaFile.setDigest(mediaOperations.digest(mediaFile));
        } catch (IOException e) {
            log.warn("Cannot calculate digest of media file {}: {}", mediaFile.getId(), e.getMessage());
        }
    }

    private void createMediaPostings() {
        for (String domainName : domains.getAllDomainNames()) {
            UUID nodeId = domains.getDomainNodeId(domainName);
            String nodeName = domains.getDomainOptions(domainName).nodeName();
            if (nodeName == null) {
                continue;
            }
            universalContext.associate(nodeId);
            List<MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository.findWithoutPosting(nodeId);
            for (MediaFileOwner mediaFileOwner : mediaFileOwners) {
                Posting posting = postingOperations.newPosting(mediaFileOwner);
                Entry parent = entryRepository.findByMediaId(mediaFileOwner.getId()).stream().findFirst().orElse(null);
                if (parent != null) {
                    posting.setAcceptedReactionsPositive(parent.getAcceptedReactionsPositive());
                    posting.setAcceptedReactionsNegative(parent.getAcceptedReactionsNegative());
                    posting.setReactionsVisible(parent.isReactionsVisible());
                    posting.setReactionTotalsVisible(parent.isReactionTotalsVisible());
                }
                mediaFileOwner.addPosting(posting);
                log.info("Created posting {} for media {}",
                        mediaFileOwner.getPosting(null).getId(), mediaFileOwner.getId());
            }
        }
    }

}
