package org.moera.node.rest;

import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.Credentials;
import org.moera.lib.node.types.CredentialsChange;
import org.moera.lib.node.types.CredentialsCreated;
import org.moera.lib.node.types.CredentialsResetToken;
import org.moera.node.data.PasswordResetTokenRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.springframework.context.MessageSource;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private Options options;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CredentialsController credentialsController;

    @BeforeEach
    void setUp() {
        when(requestContext.getOptions()).thenReturn(options);
    }

    @Test
    void disabledLoginIsReportedAsCreatedCredentials() {
        when(options.getBool("credentials.login-disabled")).thenReturn(true);

        CredentialsCreated created = credentialsController.get();

        Assertions.assertTrue(created.isCreated());
        Assertions.assertEquals(Boolean.TRUE, created.getLoginDisabled());
    }

    @Test
    void credentialsCanBeCreatedWithDisabledLoginAndWithoutPassword() {
        runOptionsTransactions();
        Credentials credentials = new Credentials();
        credentials.setLoginDisabled(true);

        credentialsController.post(credentials);

        verify(options).set("credentials.login-disabled", true);
    }

    @Test
    void credentialsChangeIsRejectedWhenLoginIsDisabled() {
        when(options.getBool("credentials.login-disabled")).thenReturn(true);
        CredentialsChange credentials = new CredentialsChange();
        credentials.setLogin("admin");
        credentials.setPassword("new-password");

        OperationFailure failure = Assertions.assertThrows(
            OperationFailure.class,
            () -> credentialsController.put(credentials)
        );

        Assertions.assertEquals("credentials.login-disabled", failure.getErrorCode());
    }

    @Test
    void credentialsResetIsRejectedWhenLoginIsDisabled() {
        when(options.getBool("credentials.login-disabled")).thenReturn(true);

        OperationFailure failure = Assertions.assertThrows(OperationFailure.class, credentialsController::reset);

        Assertions.assertEquals("credentials.login-disabled", failure.getErrorCode());
    }

    @Test
    void resetVerificationIsRejectedWhenLoginIsDisabled() {
        when(options.getBool("credentials.login-disabled")).thenReturn(true);
        CredentialsResetToken resetToken = new CredentialsResetToken();
        resetToken.setToken("123456");

        OperationFailure failure = Assertions.assertThrows(
            OperationFailure.class,
            () -> credentialsController.verifyResetToken(resetToken)
        );

        Assertions.assertEquals("credentials.login-disabled", failure.getErrorCode());
    }

    @Test
    void deletingCredentialsClearsDisabledLogin() {
        runOptionsTransactions();

        credentialsController.delete();

        verify(options).reset("credentials.login-disabled");
    }

    @SuppressWarnings("unchecked")
    private void runOptionsTransactions() {
        doAnswer(invocation -> {
            invocation.<Consumer<Options>>getArgument(0).accept(options);
            return null;
        }).when(options).runInTransaction(org.mockito.ArgumentMatchers.any());
    }

}
