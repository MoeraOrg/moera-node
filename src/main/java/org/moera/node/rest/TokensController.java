package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Password;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.global.ApiController;
import org.moera.node.model.Credentials;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.TokenCreated;
import org.moera.node.option.Options;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/tokens")
public class TokensController {

    @Inject
    private Options options;

    @Inject
    private TokenRepository tokenRepository;

    @PostMapping
    @ResponseBody
    public TokenCreated post(@Valid @RequestBody Credentials credentials) {
        if (!credentials.getLogin().equals(options.getString("credentials.login"))
            || !Password.validate(options.getString("credentials.password-hash"), credentials.getPassword())) {
            throw new OperationFailure("credentials.login-incorrect");
        }

        Token token = new Token();
        token.setToken(CryptoUtil.token());
        token.setAdmin(true);
        token.setDeadline(Timestamp.from(Instant.now().plus(options.getDuration("token.lifetime"))));
        tokenRepository.save(token);

        return new TokenCreated(token.getToken());
    }

}
