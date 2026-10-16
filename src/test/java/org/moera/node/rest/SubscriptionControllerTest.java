package org.moera.node.rest;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.OptionValueChange;
import org.moera.node.rest.task.AllRemoteSubscribersUpdateJob;
import org.moera.node.task.Jobs;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SubscriptionControllerTest {

    @ParameterizedTest
    @CsvSource({
        "public, public",
        "signed, signed",
        "admin, private",
        "subscribed, private",
        "f:00000000-0000-0000-0000-000000000001, private",
        "private, private",
        "none, private"
    })
    void visibilityChangeMapsPrincipalForRemoteSubscribers(String ourView, String theirView) {
        UUID nodeId = UUID.randomUUID();
        Jobs jobs = mock(Jobs.class);
        SubscriptionController controller = new SubscriptionController();
        ReflectionTestUtils.setField(controller, "jobs", jobs);

        controller.subscriptionVisibilityChanged(
            new OptionValueChange(nodeId, "subscriptions.view", Principal.PUBLIC, new Principal(ourView))
        );

        verify(jobs).run(
            AllRemoteSubscribersUpdateJob.class,
            new AllRemoteSubscribersUpdateJob.Parameters(new Principal(theirView)),
            nodeId
        );
    }

}
