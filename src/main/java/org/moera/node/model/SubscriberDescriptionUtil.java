package org.moera.node.model;

import org.moera.lib.node.types.SubscriberDescription;
import org.moera.lib.node.types.SubscriberOperations;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscriber;

public class SubscriberDescriptionUtil {
    
    public static SubscriberDescription build(
        SubscriptionType type,
        String feedName,
        String postingId,
        Long lastUpdatedAt,
        boolean visible
    ) {
        SubscriberDescription description = new SubscriberDescription();
        description.setType(type);
        description.setFeedName(feedName);
        description.setPostingId(postingId);
        description.setLastUpdatedAt(lastUpdatedAt);
        SubscriberOperations operations = new SubscriberOperations();
        operations.setView(visible ? Principal.PUBLIC : Principal.PRIVATE, Principal.PUBLIC);
        description.setOperations(operations);
        
        return description;
    }

    public static void toSubscriber(SubscriberDescription description, Subscriber subscriber) {
        subscriber.setSubscriptionType(description.getType());
        subscriber.setFeedName(description.getFeedName());
        Principal viewPrincipal = SubscriberOperations.getView(description.getOperations(), null);
        if (viewPrincipal != null) {
            subscriber.setViewPrincipal(viewPrincipal);
        }
    }

}
