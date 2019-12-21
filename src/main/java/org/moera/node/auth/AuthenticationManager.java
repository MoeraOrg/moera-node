package org.moera.node.auth;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.util.Util;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationManager {

    @Inject
    private TokenRepository tokenRepository;

    public boolean isAdminToken(String tokenS, UUID nodeId) throws InvalidTokenException {
        if (!StringUtils.isEmpty(tokenS)) {
            Token token = tokenRepository.findById(tokenS).orElse(null);
            if (token == null
                    || !token.getNodeId().equals(nodeId)
                    || token.getDeadline().before(Util.now())) {
                throw new InvalidTokenException();
            }
            return token.isAdmin();
        }
        return false;
    }

}
