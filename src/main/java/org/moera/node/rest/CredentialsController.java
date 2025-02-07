package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.Password;
import org.moera.node.auth.RootAdmin;
import org.moera.node.data.PasswordResetToken;
import org.moera.node.data.PasswordResetTokenRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.model.PasswordResetLiberin;
import org.moera.node.model.Credentials;
import org.moera.node.model.CredentialsChange;
import org.moera.node.model.CredentialsCreated;
import org.moera.node.model.EmailHint;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping
    @Transactional
    public CredentialsCreated get() {
        log.info("GET /credentials");

        Options options = requestContext.getOptions();
        return new CredentialsCreated(
                !ObjectUtils.isEmpty(options.getString("credentials.login"))
                && !ObjectUtils.isEmpty(options.getString("credentials.password-hash")));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Result> post(@Valid @RequestBody Credentials credentials) {
        log.info("POST /credentials (login = '{}')", credentials.getLogin());

        requestContext.getOptions().runInTransaction(options -> {
            if (!ObjectUtils.isEmpty(options.getString("credentials.login"))
                    && !ObjectUtils.isEmpty(options.getString("credentials.password-hash"))) {
                throw new OperationFailure("credentials.already-created");
            }
            options.set("credentials.login", credentials.getLogin());
            options.set("credentials.password-hash", Password.hash(credentials.getPassword()));
        });

        return ResponseEntity.created(URI.create("/credentials")).body(Result.OK);
    }

    @PutMapping
    @Transactional
    public Result put(@Valid @RequestBody CredentialsChange credentials) {
        log.info("PUT /credentials (login = '{}')", credentials.getLogin());

        if (!ObjectUtils.isEmpty(credentials.getToken())) {
            PasswordResetToken token = passwordResetTokenRepository.findById(credentials.getToken())
                    .orElseThrow(() -> new ValidationFailure("credentials.wrong-reset-token"));
            if (!token.getNodeId().equals(requestContext.nodeId())) {
                throw new ValidationFailure("credentials.wrong-reset-token");
            }
            if (token.getDeadline().before(Util.now())) {
                throw new ValidationFailure("credentials.reset-token-expired");
            }
            passwordResetTokenRepository.delete(token);
        } else {
            if (!credentials.getLogin().equals(requestContext.getOptions().getString("credentials.login"))
                    || ObjectUtils.isEmpty(credentials.getOldPassword())
                    || !Password.validate(requestContext.getOptions().getString("credentials.password-hash"),
                                          credentials.getOldPassword())) {
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
        token.setToken(CryptoUtil.token().substring(0, 10));
        token.setCreatedAt(Util.now());
        token.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("credentials-reset.token.lifetime").getDuration())));
        passwordResetTokenRepository.save(token);

        requestContext.send(new PasswordResetLiberin(token.getToken()));

        return new EmailHint(email);
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
