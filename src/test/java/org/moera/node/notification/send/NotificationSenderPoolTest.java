package org.moera.node.notification.send;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.domain.Domains;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.operations.GrantCache;
import org.moera.node.option.Options;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSenderPoolTest {

    @ParameterizedTest
    @CsvSource({"original, true", "publisher, false"})
    void evaluatesVisibilityWithTheRecipientsViewContentGrant(String grantee, boolean canViewAsAdmin) {
        UUID nodeId = UUID.randomUUID();
        Domains domains = mock(Domains.class);
        Options options = mock(Options.class);
        when(domains.getDomainOptions(nodeId)).thenReturn(options);
        when(options.nodeName()).thenReturn("publisher");
        GrantCache grants = mock(GrantCache.class);
        when(grants.get(nodeId, grantee)).thenReturn(Scope.VIEW_CONTENT.getMask());
        NotificationSenderPool pool = new NotificationSenderPool();
        ReflectionTestUtils.setField(pool, "domains", domains);
        ReflectionTestUtils.setField(pool, "grantCache", grants);
        ReflectionTestUtils.setField(pool, "subscribedCache", mock(SubscribedCache.class));
        ReflectionTestUtils.setField(pool, "friendCache", mock(FriendCache.class));
        PrincipalFilter view = mock(PrincipalFilter.class);

        pool.send(Directions.single(nodeId, "original", view), new PostingSharedNotification());

        verify(view).includes(canViewAsAdmin, "original", false, null);
    }
}
