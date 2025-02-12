package org.moera.node.model;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.SubscriberOperations;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscriber;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class SubscriberInfoUtil {

    public static SubscriberInfo build(Subscriber subscriber, Options options, AccessChecker accessChecker) {
        SubscriberInfo subscriberInfo = new SubscriberInfo();

        subscriberInfo.setId(subscriber.getId().toString());
        subscriberInfo.setType(subscriber.getSubscriptionType());
        subscriberInfo.setFeedName(subscriber.getFeedName());
        subscriberInfo.setPostingId(subscriber.getEntry() != null ? subscriber.getEntry().getId().toString() : null);
        subscriberInfo.setNodeName(subscriber.getRemoteNodeName());
        if (subscriber.getContact() != null) {
            subscriberInfo.setContact(ContactInfoUtil.build(subscriber.getContact(), options, accessChecker));
        }
        subscriberInfo.setCreatedAt(Util.toEpochSecond(subscriber.getCreatedAt()));

        SubscriberOperations baseOperations = new SubscriberOperations();
        baseOperations.setView(subscriber.getViewCompound(), Principal.PUBLIC);
        baseOperations.setDelete(subscriber.getDeletePrincipal(), Principal.PRIVATE);
        subscriberInfo.setOperations(baseOperations);

        if (accessChecker.isPrincipal(subscriber.getViewOperationsE(), Scope.SUBSCRIBE)) {
            SubscriberOperations ownerOperations = new SubscriberOperations();
            ownerOperations.setView(subscriber.getViewPrincipal(), Principal.PUBLIC);
            subscriberInfo.setOwnerOperations(ownerOperations);

            SubscriberOperations adminOperations = new SubscriberOperations();
            adminOperations.setView(subscriber.getAdminViewPrincipal(), Principal.UNSET);
            subscriberInfo.setAdminOperations(adminOperations);
        }

        return subscriberInfo;
    }

}
