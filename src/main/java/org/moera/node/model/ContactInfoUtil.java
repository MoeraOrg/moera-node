package org.moera.node.model;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.ContactOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.Contact;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.data.Subscriber;
import org.moera.node.data.UserSubscription;
import org.moera.node.option.Options;

public class ContactInfoUtil {

    public static ContactInfo build(Contact contact, Options options) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setNodeName(contact.getRemoteNodeName());
        contactInfo.setFullName(contact.getRemoteFullName());
        contactInfo.setGender(contact.getRemoteGender());
        if (contact.getRemoteAvatarMediaFile() != null) {
            contactInfo.setAvatar(
                AvatarImageUtil.build(contact.getRemoteAvatarMediaFile(), contact.getRemoteAvatarShape()));
        }
        contactInfo.setCloseness(contact.getCloseness());
        contactInfo.setHasFeedSubscriber(contact.getFeedSubscriberCount() > 0);
        contactInfo.setHasFeedSubscription(contact.getFeedSubscriptionCount() > 0);
        contactInfo.setHasFriend(contact.getFriendCount() > 0);
        contactInfo.setHasFriendOf(contact.getFriendOfCount() > 0);
        contactInfo.setHasBlock(contact.getBlockedUserCount() > 0);
        contactInfo.setHasBlockBy(contact.getBlockedByUserCount() > 0);

        ContactOperations operations = new ContactOperations();
        operations.setViewFeedSubscriber(contact.getViewFeedSubscriberCompound(options), Principal.PUBLIC);
        operations.setViewFeedSubscription(contact.getViewFeedSubscriptionCompound(options), Principal.PUBLIC);
        operations.setViewFriend(contact.getViewFriendCompound(options), Principal.PUBLIC);
        operations.setViewFriendOf(FriendOf.getViewAllE(options), Principal.PUBLIC);
        operations.setViewBlock(BlockedUser.getViewAllE(options), Principal.PUBLIC);
        operations.setViewBlockBy(BlockedByUser.getViewAllE(options), Principal.PUBLIC);
        contactInfo.setOperations(operations);

        ContactOperations ownerOperations = new ContactOperations();
        ownerOperations.setViewFeedSubscriber(contact.getViewFeedSubscriberPrincipal(), Principal.PUBLIC);
        ownerOperations.setViewFeedSubscription(contact.getViewFeedSubscriptionPrincipal(), Principal.PUBLIC);
        ownerOperations.setViewFriend(contact.getViewFriendPrincipal(), Principal.PUBLIC);
        contactInfo.setOwnerOperations(ownerOperations);

        ContactOperations adminOperations = new ContactOperations();
        adminOperations.setViewFeedSubscriber(Subscriber.getViewAllE(options), Principal.PUBLIC);
        adminOperations.setViewFeedSubscription(UserSubscription.getViewAllE(options), Principal.PUBLIC);
        adminOperations.setViewFriend(Friend.getViewAllE(options), Principal.PUBLIC);
        adminOperations.setViewFriendOf(FriendOf.getViewAllE(options), Principal.PUBLIC);
        adminOperations.setViewBlock(BlockedUser.getViewAllE(options), Principal.PUBLIC);
        adminOperations.setViewBlockBy(BlockedByUser.getViewAllE(options), Principal.PUBLIC);
        contactInfo.setAdminOperations(adminOperations);

        return contactInfo;
    }

    public static ContactInfo build(Contact contact, Options options, AccessChecker accessChecker) {
        ContactInfo contactInfo = build(contact, options);
        protect(contactInfo, accessChecker);
        return contactInfo;
    }

    public static void protect(ContactInfo contactInfo, AccessChecker accessChecker) {
        ContactOperations operations = contactInfo.getOperations();
        ContactOperations adminOperations = contactInfo.getAdminOperations();
        String ownerName = contactInfo.getNodeName();
        
        contactInfo.setHasFeedSubscriber(
            contactInfo.getHasFeedSubscriber()
                && canView(accessChecker, adminOperations.getViewFeedSubscriber(Principal.PUBLIC).withOwner(ownerName))
                && canView(accessChecker, operations.getViewFeedSubscriber(Principal.PUBLIC).withOwner(ownerName))
        );
        contactInfo.setHasFeedSubscription(
            contactInfo.getHasFeedSubscription()
                && canView(accessChecker, adminOperations.getViewFeedSubscription(Principal.PUBLIC).withOwner(ownerName))
                && canView(accessChecker, operations.getViewFeedSubscription(Principal.PUBLIC).withOwner(ownerName))
        );
        contactInfo.setHasFriend(
            contactInfo.getHasFriend()
                && canView(accessChecker, adminOperations.getViewFriend(Principal.PUBLIC).withOwner(ownerName))
                && canView(accessChecker, operations.getViewFriend(Principal.PUBLIC).withOwner(ownerName))
        );
        contactInfo.setHasFriendOf(
            contactInfo.getHasFriendOf()
                && canView(accessChecker, adminOperations.getViewFriendOf(Principal.PUBLIC).withOwner(ownerName))
        );
        contactInfo.setHasBlock(
            contactInfo.getHasBlock()
                && canView(accessChecker, adminOperations.getViewBlock(Principal.PUBLIC).withOwner(ownerName))
        );
        contactInfo.setHasBlockBy(
            contactInfo.getHasBlockBy()
                && canView(accessChecker, adminOperations.getViewBlock(Principal.PUBLIC).withOwner(ownerName))
        );
    }

    private static boolean canView(AccessChecker accessChecker, Principal principal) {
        return accessChecker.isPrincipal(principal, Scope.VIEW_PEOPLE);
    }

}
