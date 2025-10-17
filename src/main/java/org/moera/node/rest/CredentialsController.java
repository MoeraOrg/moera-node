package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.Password;
import org.moera.lib.node.types.Credentials;
import org.moera.lib.node.types.CredentialsChange;
import org.moera.lib.node.types.CredentialsCreated;
import org.moera.lib.node.types.CredentialsResetToken;
import org.moera.lib.node.types.EmailHint;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.VerificationInfo;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.RootAdmin;
import org.moera.node.data.PasswordResetToken;
import org.moera.node.data.PasswordResetTokenRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.model.PasswordResetLiberin;
import org.moera.node.model.CredentialsCreatedUtil;
import org.moera.node.model.EmailHintUtil;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.VerificationInfoUtil;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/credentials")
@NoCache
public class CredentialsController {

    private static final Logger log = LoggerFactory.getLogger(CredentialsController.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject
    private MessageSource messageSource;

    @GetMapping
    @Transactional
    public CredentialsCreated get() {
        log.info("GET /credentials");

        Options options = requestContext.getOptions();
        return CredentialsCreatedUtil.build(
            !ObjectUtils.isEmpty(options.getString("credentials.login"))
            && !ObjectUtils.isEmpty(options.getString("credentials.password-hash"))
        );
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Result> post(@RequestBody Credentials credentials) {
        log.info("POST /credentials (login = {})", LogUtil.format(credentials.getLogin()));

        credentials.validate();

        requestContext.getOptions().runInTransaction(options -> {
            if (
                !ObjectUtils.isEmpty(options.getString("credentials.login"))
                && !ObjectUtils.isEmpty(options.getString("credentials.password-hash"))
            ) {
                throw new OperationFailure("credentials.already-created");
            }
            options.set("credentials.login", credentials.getLogin());
            options.set("credentials.password-hash", Password.hash(credentials.getPassword()));
        });

        return ResponseEntity.created(URI.create("/credentials")).body(Result.OK);
    }

    @PutMapping
    @Transactional
    public Result put(@RequestBody CredentialsChange credentials) {
        log.info("PUT /credentials (login = {})", LogUtil.format(credentials.getLogin()));

        credentials.validate();

        if (!ObjectUtils.isEmpty(credentials.getToken())) {
            PasswordResetToken token = passwordResetTokenRepository.findById(credentials.getToken())
                .orElseThrow(() -> new ValidationFailure("credentials.wrong-reset-token"));
            ValidationUtil.assertion(
                token.getNodeId().equals(requestContext.nodeId()),
                "credentials.wrong-reset-token"
            );
            ValidationUtil.assertion(token.getDeadline().after(Util.now()), "credentials.reset-token-expired");
            passwordResetTokenRepository.delete(token);
        } else {
            if (
                !credentials.getLogin().equals(requestContext.getOptions().getString("credentials.login"))
                || ObjectUtils.isEmpty(credentials.getOldPassword())
                || !Password.validate(
                    requestContext.getOptions().getString("credentials.password-hash"), credentials.getOldPassword()
                )
            ) {
                throw new OperationFailure("credentials.login-incorrect");
            }
        }
        requestContext.getOptions().runInTransaction(options -> {
            options.set("credentials.login", credentials.getLogin());
            options.set("credentials.password-hash", Password.hash(credentials.getPassword()));
        });

        return Result.OK;
    }

    @DeleteMapping
    @RootAdmin
    @Transactional
    public Result delete() {
        log.info("DELETE /credentials");

        requestContext.getOptions().runInTransaction(options -> {
            options.reset("credentials.login");
            options.reset("credentials.password-hash");
        });

        return Result.OK;
    }

    @PostMapping("/reset")
    @Transactional
    public EmailHint reset() {
        log.info("POST /credentials/reset");

        String email = requestContext.getOptions().getString("profile.email");
        if (ObjectUtils.isEmpty(email)) {
            throw new OperationFailure("credentials.email-not-set");
        }

        PasswordResetToken token = new PasswordResetToken();
        token.setNodeId(requestContext.nodeId());
        token.setToken(CryptoUtil.humanFriendlyToken(6));
        token.setCreatedAt(Util.now());
        token.setDeadline(
            Timestamp.from(
                Instant
                    .now()
                    .plus(requestContext.getOptions().getDuration("credentials-reset.token.lifetime").getDuration())
            )
        );
        passwordResetTokenRepository.save(token);

        requestContext.send(new PasswordResetLiberin(token.getToken()));

        return EmailHintUtil.build(email);
    }

    @PostMapping("/reset/verify")
    @Transactional
    public VerificationInfo verifyResetToken(@RequestBody CredentialsResetToken resetToken) {
        log.info("POST /credentials/reset/verify");

        resetToken.validate();

        PasswordResetToken token = passwordResetTokenRepository.findById(resetToken.getToken()).orElse(null);
        if (token == null || !token.getNodeId().equals(requestContext.nodeId())) {
            return VerificationInfoUtil.incorrect("credentials.wrong-reset-token", messageSource);
        }
        if (!token.getDeadline().after(Util.now())) {
            return VerificationInfoUtil.incorrect("credentials.reset-token-expired", messageSource);
        }

        return VerificationInfoUtil.correct();
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired password reset tokens");

            passwordResetTokenRepository.deleteExpired(Util.now());
        }
    }

}
