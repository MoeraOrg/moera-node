package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.Password;
import org.moera.lib.node.types.DomainInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.TokenInfo;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.model.DefrostLiberin;
import org.moera.node.liberin.model.TokenAddedLiberin;
import org.moera.node.liberin.model.TokenDeletedLiberin;
import org.moera.node.liberin.model.TokenUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.TokenAttributes;
import org.moera.node.model.TokenInfoUtil;
import org.moera.node.model.TokenUpdate;
import org.moera.node.notification.receive.DefrostNotificationsJob;
import org.moera.node.option.Options;
import org.moera.node.task.Jobs;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/tokens")
@NoCache
public class TokenController {

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private TokenRepository tokenRepository;

    @Inject
    private Domains domains;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Transactional
    public ResponseEntity<TokenInfo> post(@Valid @RequestBody TokenAttributes attributes) {
        log.info("POST /tokens (login = '{}')", attributes.getLogin());

        Options options = requestContext.getOptions();
        if (ObjectUtils.isEmpty(options.getString("credentials.login"))
                || ObjectUtils.isEmpty(options.getString("credentials.password-hash"))) {
            throw new OperationFailure("credentials.not-created");
        }
        if (!attributes.getLogin().equals(options.getString("credentials.login"))
            || !Password.validate(options.getString("credentials.password-hash"), attributes.getPassword())) {
            throw new OperationFailure("credentials.login-incorrect");
        }

        Token token = new Token();
        token.setId(UUID.randomUUID());
        token.setNodeId(options.nodeId());
        token.setName(attributes.getName());
        token.setToken(CryptoUtil.token());
        token.setAuthScope(attributes.getPermissions() != null
                ? Scope.forValues(attributes.getPermissions())
                : Scope.ALL.getMask());
        token.setDeadline(Timestamp.from(Instant.now().plus(
                options.getDuration("token.lifetime").getDuration())));
        tokenRepository.save(token);

        if (requestContext.getOptions().isFrozen()) {
            requestContext.getOptions().set("frozen", false);
            jobs.run(DefrostNotificationsJob.class, new DefrostNotificationsJob.Parameters(), options.nodeId());
            requestContext.send(new DefrostLiberin());
        }

        requestContext.send(new TokenAddedLiberin(token));

        return ResponseEntity.created(URI.create("/tokens/" + token.getId())).body(TokenInfoUtil.build(token, true));
    }

    @PutMapping("/{id}")
    @Admin(Scope.TOKENS)
    @Transactional
    public TokenInfo put(@PathVariable UUID id, @Valid @RequestBody TokenUpdate update) {
        log.info("PUT /tokens/{} (name = {})", id, LogUtil.format(update.getName()));

        Token token = tokenRepository.findByNodeIdAndId(requestContext.nodeId(), id, Util.now()).orElse(null);
        if (token == null) {
            throw new ObjectNotFoundFailure("not-found");
        }
        if (!ObjectUtils.isEmpty(update.getName())) {
            token.setName(update.getName());
        }
        if (update.getPermissions() != null) {
            long updatedScope = Scope.forValues(update.getPermissions());
            token.setAuthScope(token.getAuthScope() & updatedScope);
        }

        requestContext.send(new TokenUpdatedLiberin(token));

        return TokenInfoUtil.build(token, false);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.TOKENS)
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /tokens/{}", id);

        Token token = tokenRepository.findByNodeIdAndId(requestContext.nodeId(), id, Util.now()).orElse(null);
        if (token == null) {
            throw new ObjectNotFoundFailure("not-found");
        }
        tokenRepository.delete(token);

        requestContext.send(new TokenDeletedLiberin(token.getId()));

        return Result.OK;
    }

    @GetMapping
    @Admin(Scope.TOKENS)
    @Transactional
    public List<TokenInfo> getAll() {
        log.info("GET /tokens");

        List<Token> tokens = tokenRepository.findAllByNodeId(requestContext.nodeId(), Util.now());
        return tokens.stream().map(td -> TokenInfoUtil.build(td, false)).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Admin(Scope.TOKENS)
    @Transactional
    public TokenInfo get(@PathVariable UUID id) {
        log.info("GET /tokens/{}", id);

        Token token = tokenRepository.findByNodeIdAndId(requestContext.nodeId(), id, Util.now()).orElse(null);
        if (token == null) {
            throw new ObjectNotFoundFailure("not-found");
        }
        return TokenInfoUtil.build(token, false);
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired tokens");

            tokenRepository.deleteExpired(Util.now());
        }
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void freeze() {
        try (var ignored = requestCounter.allot()) {
            log.info("Freezing inactive domains");

            domains.getAllDomainNames().stream()
                    .map(domains::getDomain)
                    .filter(info -> Instant.ofEpochSecond(info.getCreatedAt())
                            .plus(365, ChronoUnit.DAYS)
                            .isBefore(Instant.now()))
                    .map(DomainInfo::getNodeId)
                    .map(domains::getDomainOptions)
                    .filter(Objects::nonNull)
                    .filter(options -> !options.isFrozen())
                    .filter(options -> tokenRepository.countAllByNodeId(options.nodeId(), Util.now()) == 0)
                    .forEach(options -> options.set("frozen", true));
        }
    }

}
