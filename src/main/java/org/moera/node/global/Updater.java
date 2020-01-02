package org.moera.node.global;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionUpgrade;
import org.moera.node.data.EntryRevisionUpgradeRepository;
import org.moera.node.data.Posting;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.Body;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Updater {

    private static Logger log = LoggerFactory.getLogger(Updater.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private Domains domains;

    @Inject
    private EntryRevisionUpgradeRepository entryRevisionUpgradeRepository;

    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void execute() {
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
        if (StringUtils.isEmpty(options.nodeName())) {
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

}
