package org.moera.node.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.time.Duration;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.crypto.Password;
import org.moera.lib.node.types.TokenAttributes;
import org.moera.lib.node.types.TokenInfo;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private Options options;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenController tokenController;

    @BeforeAll
    static void setUpCrypto() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void tokenCreationDoesNotPersistTheReturnedSecret() throws NoSuchAlgorithmException {
        when(requestContext.getOptions()).thenReturn(options);
        when(options.nodeId()).thenReturn(UUID.randomUUID());
        when(options.getString("credentials.login")).thenReturn("admin");
        when(options.getString("credentials.password-hash")).thenReturn(Password.hash("password"));
        when(options.getDuration("token.lifetime")).thenReturn(new ExtendedDuration(Duration.ofDays(1)));
        TokenAttributes attributes = new TokenAttributes();
        attributes.setLogin("admin");
        attributes.setPassword("password");

        TokenInfo response = tokenController.post(attributes).getBody();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(43, response.getToken().length());
        ArgumentCaptor<Token> stored = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(stored.capture());
        Assertions.assertArrayEquals(
            MessageDigest.getInstance("SHA-256").digest(response.getToken().getBytes(StandardCharsets.UTF_8)),
            stored.getValue().getTokenHash()
        );
        Assertions.assertEquals(response.getToken().substring(0, 4), stored.getValue().getTokenPrefix());
    }

    @Test
    void tokenCreationIsRejectedWhenLoginIsDisabled() {
        when(requestContext.getOptions()).thenReturn(options);
        when(options.getBool("credentials.login-disabled")).thenReturn(true);
        TokenAttributes attributes = new TokenAttributes();
        attributes.setLogin("admin");
        attributes.setPassword("password");

        OperationFailure failure = Assertions.assertThrows(
            OperationFailure.class,
            () -> tokenController.post(attributes)
        );

        Assertions.assertEquals("credentials.login-disabled", failure.getErrorCode());
    }

}
