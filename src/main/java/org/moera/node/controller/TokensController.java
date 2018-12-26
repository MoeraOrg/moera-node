package org.moera.node.controller;

import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;

import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Password;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.model.Credentials;
import org.moera.node.model.Success;
import org.moera.node.option.Options;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera-node/tokens")
public class TokensController {

    @Inject
    private Options options;

    @Inject
    private TokenRepository tokenRepository;

    @PostMapping
    @ResponseBody
    public Object post(@RequestBody Credentials credentials) throws NoSuchAlgorithmException {
        if (StringUtils.isEmpty(credentials.getLogin()) || StringUtils.isEmpty(credentials.getPassword())
            || !credentials.getLogin().equals(options.getString("credentials.login"))
            || !Password.validate(options.getString("credentials.password-hash"), credentials.getPassword())) {
            return new Success(1, "login incorrect");
        }

        Token token = new Token();
        token.setToken(CryptoUtil.token());
        token.setAdmin(true);
        token.setDeadline(Timestamp.from(Instant.now().plus(options.getDuration("token.lifetime"))));
        tokenRepository.save(token);

        return new org.moera.node.model.Token(token.getToken());
    }

}
