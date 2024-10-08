package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriberInfo {

    private String id;
    private SubscriptionType type;
    private String feedName;
    private String postingId;
    private String nodeName;
    private ContactInfo contact;
    private Long createdAt;
    private Map<String, Principal> operations;
    private Map<String, Principal> ownerOperations;
    private Map<String, Principal> adminOperations;

    public SubscriberInfo() {
    }

    public SubscriberInfo(Subscriber subscriber, Options options, AccessChecker accessChecker) {
        id = subscriber.getId().toString();
        type = subscriber.getSubscriptionType();
        feedName = subscriber.getFeedName();
        postingId = subscriber.getEntry() != null ? subscriber.getEntry().getId().toString() : null;
        nodeName = subscriber.getRemoteNodeName();
        if (subscriber.getContact() != null) {
            contact = new ContactInfo(subscriber.getContact(), options, accessChecker);
        }
        createdAt = Util.toEpochSecond(subscriber.getCreatedAt());

        operations = new HashMap<>();
        putOperation(operations, "view", subscriber.getViewCompound(), Principal.PUBLIC);
        putOperation(operations, "delete", subscriber.getDeletePrincipal(), Principal.PRIVATE);

        if (accessChecker.isPrincipal(subscriber.getViewOperationsE(), Scope.SUBSCRIBE)) {
            ownerOperations = new HashMap<>();
            putOperation(ownerOperations, "view", subscriber.getViewPrincipal(), Principal.PUBLIC);

            adminOperations = new HashMap<>();
            putOperation(adminOperations, "view", subscriber.getAdminViewPrincipal(), Principal.UNSET);
        }
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public ContactInfo getContact() {
        return contact;
    }

    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getOwnerOperations() {
        return ownerOperations;
    }

    public void setOwnerOperations(Map<String, Principal> ownerOperations) {
        this.ownerOperations = ownerOperations;
    }

    public Map<String, Principal> getAdminOperations() {
        return adminOperations;
    }

    public void setAdminOperations(Map<String, Principal> adminOperations) {
        this.adminOperations = adminOperations;
    }

}
