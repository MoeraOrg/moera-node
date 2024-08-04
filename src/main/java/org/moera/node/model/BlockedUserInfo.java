package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.SourceFormat;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedUserInfo {

    private String id;
    private BlockedOperation blockedOperation;
    private String nodeName;
    private ContactInfo contact;
    private String entryId;
    private String entryNodeName;
    private String entryPostingId;
    private long createdAt;
    private Long deadline;
    private String reasonSrc;
    private SourceFormat reasonSrcFormat;
    private String reason;

    public BlockedUserInfo() {
    }

    public BlockedUserInfo(BlockedUser blockedUser, Options options) {
        id = blockedUser.getId().toString();
        blockedOperation = blockedUser.getBlockedOperation();
        nodeName = blockedUser.getRemoteNodeName();
        if (blockedUser.getContact() != null) {
            contact = new ContactInfo(blockedUser.getContact(), options);
        }
        if (blockedUser.getEntry() != null) {
            entryId = blockedUser.getEntry().getId().toString();
        }
        entryNodeName = blockedUser.getEntryNodeName();
        entryPostingId = blockedUser.getEntryPostingId();
        createdAt = Util.toEpochSecond(blockedUser.getCreatedAt());
        deadline = Util.toEpochSecond(blockedUser.getDeadline());
        reasonSrc = blockedUser.getReasonSrc();
        reasonSrcFormat = blockedUser.getReasonSrcFormat();
        reason = blockedUser.getReason();
    }

    public BlockedUserInfo(BlockedUser blockedUser, Options options, AccessChecker accessChecker) {
        this(blockedUser, options);
        protect(accessChecker);
    }

    public void protect(AccessChecker accessChecker) {
        contact.protect(accessChecker);
        if (!accessChecker.isPrincipal(Principal.ADMIN, Scope.VIEW_PEOPLE)) {
            reasonSrc = null;
            reasonSrcFormat = null;
        }
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

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getEntryNodeName() {
        return entryNodeName;
    }

    public void setEntryNodeName(String entryNodeName) {
        this.entryNodeName = entryNodeName;
    }

    public String getEntryPostingId() {
        return entryPostingId;
    }

    public void setEntryPostingId(String entryPostingId) {
        this.entryPostingId = entryPostingId;
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

    public String getReasonSrc() {
        return reasonSrc;
    }

    public void setReasonSrc(String reasonSrc) {
        this.reasonSrc = reasonSrc;
    }

    public SourceFormat getReasonSrcFormat() {
        return reasonSrcFormat;
    }

    public void setReasonSrcFormat(SourceFormat reasonSrcFormat) {
        this.reasonSrcFormat = reasonSrcFormat;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
