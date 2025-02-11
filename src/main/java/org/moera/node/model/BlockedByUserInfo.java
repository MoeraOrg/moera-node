package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.BlockedByUser;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedByUserInfo {

    private String id;
    private BlockedOperation blockedOperation;
    private ContactInfo contact;
    private String nodeName;
    private String postingId;
    private long createdAt;
    private Long deadline;
    private String reason;

    public BlockedByUserInfo() {
    }

    public BlockedByUserInfo(BlockedByUser blockedByUser, Options options) {
        id = blockedByUser.getId().toString();
        blockedOperation = blockedByUser.getBlockedOperation();
        if (blockedByUser.getContact() != null) {
            contact = new ContactInfo(blockedByUser.getContact(), options);
        }
        nodeName = blockedByUser.getRemoteNodeName();
        postingId = blockedByUser.getRemotePostingId();
        createdAt = Util.toEpochSecond(blockedByUser.getCreatedAt());
        deadline = Util.toEpochSecond(blockedByUser.getDeadline());
        reason = blockedByUser.getReason();
    }

    public BlockedByUserInfo(BlockedByUser blockedByUser, Options options, AccessChecker accessChecker) {
        this(blockedByUser, options);
        protect(accessChecker);
    }

    public void protect(AccessChecker accessChecker) {
        contact.protect(accessChecker);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BlockedOperation getBlockedOperation() {
        return blockedOperation;
    }

    public void setBlockedOperation(BlockedOperation blockedOperation) {
        this.blockedOperation = blockedOperation;
    }

    public ContactInfo getContact() {
        return contact;
    }

    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
