package org.moera.node.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.TokenAttributes;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private Options options;

    @InjectMocks
    private TokenController tokenController;

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
