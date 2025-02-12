package org.moera.node.model;

import org.moera.lib.node.types.SubscriptionInfo;
import org.moera.lib.node.types.SubscriptionOperations;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class SubscriptionInfoUtil {

    public static SubscriptionInfo build(UserSubscription subscription, Options options) {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setId(subscription.getId().toString());
        subscriptionInfo.setType(subscription.getSubscriptionType());
        subscriptionInfo.setFeedName(subscription.getFeedName());
        subscriptionInfo.setRemoteNodeName(subscription.getRemoteNodeName());
        if (subscription.getContact() != null) {
            subscriptionInfo.setContact(ContactInfoUtil.build(subscription.getContact(), options));
        }
        subscriptionInfo.setRemoteFeedName(subscription.getRemoteFeedName());
        subscriptionInfo.setRemotePostingId(subscription.getRemoteEntryId());
        subscriptionInfo.setCreatedAt(Util.toEpochSecond(subscription.getCreatedAt()));
        subscriptionInfo.setReason(subscription.getReason());

        SubscriptionOperations operations = new SubscriptionOperations();
        operations.setView(subscription.getViewPrincipal(), Principal.PUBLIC);
        operations.setDelete(subscription.getDeletePrincipal(options), Principal.ADMIN);
        subscriptionInfo.setOperations(operations);

        return subscriptionInfo;
    }

    public static SubscriptionInfo build(UserSubscription subscription, Options options, AccessChecker accessChecker) {
        SubscriptionInfo subscriptionInfo = build(subscription, options);
        protect(subscriptionInfo, accessChecker);
        return subscriptionInfo;
    }

    public static void protect(SubscriptionInfo subscriptionInfo, AccessChecker accessChecker) {
        if (subscriptionInfo.getContact() != null) {
            ContactInfoUtil.protect(subscriptionInfo.getContact(), accessChecker);
        }
    }

}
