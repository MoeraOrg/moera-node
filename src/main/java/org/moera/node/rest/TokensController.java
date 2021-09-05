package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Password;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Credentials;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.TokenCreated;
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

    private static Logger log = LoggerFactory.getLogger(TokensController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TokenRepository tokenRepository;

    @PostMapping
    public ResponseEntity<TokenCreated> post(@Valid @RequestBody Credentials credentials) {
        log.info("POST /tokens (login = '{}')", credentials.getLogin());

        Options options = requestContext.getOptions();
        if (ObjectUtils.isEmpty(options.getString("credentials.login"))
                || ObjectUtils.isEmpty(options.getString("credentials.password-hash"))) {
            throw new OperationFailure("credentials.not-created");
        }
        if (!credentials.getLogin().equals(options.getString("credentials.login"))
            || !Password.validate(options.getString("credentials.password-hash"), credentials.getPassword())) {
            throw new OperationFailure("credentials.login-incorrect");
        }

        Token token = new Token();
        token.setNodeId(options.nodeId());
        token.setToken(CryptoUtil.token());
        token.setAdmin(true);
        token.setDeadline(Timestamp.from(Instant.now().plus(
                options.getDuration("token.lifetime").getDuration())));
        tokenRepository.save(token);

        return ResponseEntity.created(URI.create("/tokens/" + token.getToken()))
                .body(new TokenCreated(token.getToken(), "admin"));
    }

    @GetMapping("/{token}")
    public TokenInfo get(@PathVariable String token) {
        log.info("GET /tokens/{}", token);

        Token tokenData = tokenRepository.findById(token).orElse(null);
        if (tokenData == null || !tokenData.getNodeId().equals(requestContext.nodeId())) {
            return new TokenInfo(token, false);
        }
        return new TokenInfo(tokenData);
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteExpired(Util.now());
    }

}
