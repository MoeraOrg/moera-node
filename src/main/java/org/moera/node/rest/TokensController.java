package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Password;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthCategory;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.TokenAttributes;
import org.moera.node.model.TokenInfo;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/tokens")
@NoCache
public class TokensController {

    private static final Logger log = LoggerFactory.getLogger(TokensController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TokenRepository tokenRepository;

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
        token.setToken(CryptoUtil.token());
        token.setAuthCategory(attributes.getAuthCategory() != null ? attributes.getAuthCategory() : AuthCategory.ALL);
        token.setDeadline(Timestamp.from(Instant.now().plus(
                options.getDuration("token.lifetime").getDuration())));
        tokenRepository.save(token);

        return ResponseEntity.created(URI.create("/tokens/" + token.getId())).body(new TokenInfo(token, true));
    }

    @GetMapping
    @Admin
    @Transactional
    public List<TokenInfo> getAll() {
        log.info("GET /tokens");

        List<Token> tokens = tokenRepository.findAllByNodeId(requestContext.nodeId(), Util.now());
        return tokens.stream().map(td -> new TokenInfo(td, false)).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Admin
    @Transactional
    public TokenInfo get(@PathVariable UUID id) {
        log.info("GET /tokens/{}", id);

        Token tokenData = tokenRepository.findByNodeIdAndId(requestContext.nodeId(), id, Util.now()).orElse(null);
        if (tokenData == null) {
            throw new ObjectNotFoundFailure("not-found");
        }
        return new TokenInfo(tokenData, false);
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteExpired(Util.now());
    }

}
