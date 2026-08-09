package org.moera.node.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthSecretsTest {

    @Test
    void parsesSupportedAuthenticationFormats() {
        Assertions.assertEquals(new AuthSecrets("root", null, null), new AuthSecrets("secret:root"));
        Assertions.assertEquals(new AuthSecrets(null, "token", null), new AuthSecrets("token:token"));
        Assertions.assertEquals(new AuthSecrets(null, null, "carte"), new AuthSecrets("carte:carte"));
        Assertions.assertEquals(new AuthSecrets(null, "legacy", null), new AuthSecrets("legacy"));
        Assertions.assertEquals(new AuthSecrets(null, null, null), new AuthSecrets());
    }

}
