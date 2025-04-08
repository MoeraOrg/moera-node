package org.moera.node.model;

import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.SubscriptionDescription;
import org.moera.lib.node.types.SubscriptionOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;

public class SubscriptionDescriptionUtil {

    public static void toUserSubscription(SubscriptionDescription description, UserSubscription subscription) {
        subscription.setSubscriptionType(description.getType());
        subscription.setFeedName(description.getFeedName());
        subscription.setRemoteNodeName(NodeName.expand(description.getRemoteNodeName()));
        subscription.setRemoteFeedName(description.getRemoteFeedName());
        subscription.setRemoteEntryId(description.getRemotePostingId());
        if (description.getReason() != null) {
            subscription.setReason(description.getReason());
        }
        Principal viewPrincipal = SubscriptionOperations.getView(description.getOperations(), null);
        if (viewPrincipal != null) {
            subscription.setViewPrincipal(viewPrincipal);
        }
    }

}
