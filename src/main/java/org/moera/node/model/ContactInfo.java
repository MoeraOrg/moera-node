package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Contact;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.data.Subscriber;
import org.moera.node.data.UserSubscription;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactInfo {

    private String nodeName;
    private String fullName;
    private String gender;
    private AvatarImage avatar;
    private float closeness;
    private boolean hasFeedSubscriber;
    private boolean hasFeedSubscription;
    private boolean hasFriend;
    private boolean hasFriendOf;
    private Map<String, Principal> operations;
    private Map<String, Principal> adminOperations;

    public ContactInfo() {
    }

    public ContactInfo(Contact contact, Options options) {
        nodeName = contact.getRemoteNodeName();
        fullName = contact.getRemoteFullName();
        gender = contact.getRemoteGender();
        if (contact.getRemoteAvatarMediaFile() != null) {
            avatar = new AvatarImage(contact.getRemoteAvatarMediaFile(), contact.getRemoteAvatarShape());
        }
        closeness = contact.getCloseness();
        hasFeedSubscriber = contact.getFeedSubscriberCount() > 0;
        hasFeedSubscription = contact.getFeedSubscriptionCount() > 0;
        hasFriend = contact.getFriendCount() > 0;
        hasFriendOf = contact.getFriendOfCount() > 0;

        operations = new HashMap<>();
        putOperation(operations, "viewFeedSubscriber", contact.getViewFeedSubscriberPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewFeedSubscription", contact.getViewFeedSubscriptionPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewFriend", contact.getViewFriendPrincipal(), Principal.PUBLIC);

        adminOperations = new HashMap<>();
        putOperation(adminOperations, "viewFeedSubscriber", Subscriber.getViewAllE(options), Principal.PUBLIC);
        putOperation(adminOperations, "viewFeedSubscription", UserSubscription.getViewAllE(options), Principal.PUBLIC);
        putOperation(adminOperations, "viewFriend", Friend.getViewAllE(options), Principal.PUBLIC);
        putOperation(adminOperations, "viewFriendOf", FriendOf.getViewAllE(options), Principal.PUBLIC);
    }

    public ContactInfo(Contact contact, Options options, AccessChecker accessChecker) {
        this(contact, options);
        protect(accessChecker);
    }

    public void protect(AccessChecker accessChecker) {
        hasFeedSubscriber = hasFeedSubscriber
                && accessChecker.isPrincipal(adminOperations.getOrDefault("viewFeedSubscriber", Principal.PUBLIC))
                && accessChecker.isPrincipal(operations.getOrDefault("viewFeedSubscriber", Principal.PUBLIC));
        hasFeedSubscription = hasFeedSubscription
                && accessChecker.isPrincipal(adminOperations.getOrDefault("viewFeedSubscription", Principal.PUBLIC))
                && accessChecker.isPrincipal(operations.getOrDefault("viewFeedSubscription", Principal.PUBLIC));
        hasFriend = hasFriend
                && accessChecker.isPrincipal(adminOperations.getOrDefault("viewFriend", Principal.PUBLIC))
                && accessChecker.isPrincipal(operations.getOrDefault("viewFriend", Principal.PUBLIC));
        hasFriendOf = hasFriendOf
                && accessChecker.isPrincipal(adminOperations.getOrDefault("viewFriendOf", Principal.PUBLIC));
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    public float getCloseness() {
        return closeness;
    }

    public void setCloseness(float closeness) {
        this.closeness = closeness;
    }

    public boolean isHasFeedSubscriber() {
        return hasFeedSubscriber;
    }

    public void setHasFeedSubscriber(boolean hasFeedSubscriber) {
        this.hasFeedSubscriber = hasFeedSubscriber;
    }

    public boolean isHasFeedSubscription() {
        return hasFeedSubscription;
    }

    public void setHasFeedSubscription(boolean hasFeedSubscription) {
        this.hasFeedSubscription = hasFeedSubscription;
    }

    public boolean isHasFriend() {
        return hasFriend;
    }

    public void setHasFriend(boolean hasFriend) {
        this.hasFriend = hasFriend;
    }

    public boolean isHasFriendOf() {
        return hasFriendOf;
    }

    public void setHasFriendOf(boolean hasFriendOf) {
        this.hasFriendOf = hasFriendOf;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getAdminOperations() {
        return adminOperations;
    }

    public void setAdminOperations(Map<String, Principal> adminOperations) {
        this.adminOperations = adminOperations;
    }

}
