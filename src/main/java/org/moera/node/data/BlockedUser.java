package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "blocked_users")
public class BlockedUser implements ContactRelated {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private BlockedOperation blockedOperation;

    @NotNull
    private String remoteNodeName;

    @ManyToOne
    private Contact contact;

    @ManyToOne
    private Entry entry;

    private String entryNodeName;

    private String entryPostingId;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deadline;

    @NotNull
    private String reasonSrc = "";

    @NotNull
    @Enumerated
    private SourceFormat reasonSrcFormat = SourceFormat.MARKDOWN;

    @NotNull
    private String reason = "";

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public BlockedOperation getBlockedOperation() {
        return blockedOperation;
    }

    public void setBlockedOperation(BlockedOperation blockedOperation) {
        this.blockedOperation = blockedOperation;
    }

    @Override
    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
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

    public boolean isGlobal() {
        return entry == null && entryNodeName == null && entryPostingId == null;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
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

    private Principal toAbsolute(Principal principal) {
        return principal.withOwner(getRemoteNodeName());
    }

    public static Principal getViewAllPrincipal(Options options) {
        return options.getPrincipal("blocked-users.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    @Override
    public void toContactViewPrincipal(Contact contact) {
    }

}
