package org.moera.node.global;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SignatureUpdater {

    private static Logger log = LoggerFactory.getLogger(SignatureUpdater.class);

    @Inject
    private Domains domains;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void load() {
        domains.getAllDomainNames().stream().map(domains::getDomainOptions).forEach(this::processDomain);
    }

    private void processDomain(Options options) {
        String ownerName = options.getString("profile.registered-name");
        if (StringUtils.isEmpty(ownerName)) {
            return;
        }
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            return;
        }
        int totalUpdated = 0;
        for (EntryRevision revision : entryRevisionRepository.findUnsigned(options.nodeId(), ownerName)) {
            Posting posting = (Posting) revision.getEntry();
            PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
            revision.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
            totalUpdated++;
        }
        if (totalUpdated != 0) {
            log.info("Updated {} signatures in domain {}", totalUpdated, options.nodeId());
        }
    }

}
