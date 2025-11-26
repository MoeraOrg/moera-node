package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.node.data.EmailVerification;
import org.moera.node.data.EmailVerificationRepository;
import org.moera.node.data.VerifiedEmail;
import org.moera.node.data.VerifiedEmailRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.EmailVerificationLiberin;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class EmailVerificationOperations {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationOperations.class);

    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Domains domains;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private EmailVerificationRepository emailVerificationRepository;

    @Inject
    private VerifiedEmailRepository verifiedEmailRepository;

    @Inject
    private Transaction tx;

    public String createVerification(UUID nodeId, String email) {
        String token = CryptoUtil.token();
        Timestamp deadline = Timestamp.from(Instant.now().plus(VERIFICATION_TTL));

        tx.executeWrite(() -> {
            EmailVerification verification = new EmailVerification(nodeId, email, token, deadline);
            emailVerificationRepository.save(verification);
        });

        return token;
    }

    public boolean verified(String token) {
        return tx.executeWrite(() -> {
            EmailVerification verification = emailVerificationRepository.findByToken(token, Util.now()).orElse(null);
            if (verification == null) {
                return false;
            }

            boolean exists = verifiedEmailRepository.countByNodeIdAndEmail(
                verification.getNodeId(), verification.getEmail()
            ) > 0;
            if (!exists) {
                VerifiedEmail verifiedEmail = new VerifiedEmail(verification.getNodeId(), verification.getEmail());
                verifiedEmailRepository.save(verifiedEmail);
            }
            var options = domains.getDomainOptions(verification.getNodeId());
            if (Objects.equals(options.getString("profile.email"), verification.getEmail())) {
                options.set("profile.email.verified", true);
            }
            emailVerificationRepository.delete(verification);

            return true;
        });
    }

    @OptionHook("profile.email")
    public void profileEmailChanged(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        String email = (String) change.getNewValue();
        if (ObjectUtils.isEmpty(email)) {
            return;
        }

        boolean verified = tx.executeRead(() ->
            verifiedEmailRepository.countByNodeIdAndEmail(change.getNodeId(), email) > 0
        );
        universalContext.getOptions().set("profile.email.verified", verified);

        if (!verified) {
            String token = createVerification(change.getNodeId(), email);
            universalContext.send(new EmailVerificationLiberin(universalContext.nodeName(), token));
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void deleteExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Deleting expired email verifications");

            tx.executeWrite(() -> emailVerificationRepository.deleteExpired(Util.now()));
        }
    }

}
