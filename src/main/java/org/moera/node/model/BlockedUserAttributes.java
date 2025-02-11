package org.moera.node.model;

import java.util.UUID;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.SourceFormat;
import org.moera.node.data.BlockedUser;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class BlockedUserAttributes {

    private BlockedOperation blockedOperation;

    private String nodeName;

    private UUID entryId;

    private String entryNodeName;

    private String entryPostingId;

    private Long deadline;

    @Size(max = 4096)
    private String reasonSrc;

    private SourceFormat reasonSrcFormat = SourceFormat.MARKDOWN;

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

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
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

    public void toBlockedInstant(BlockedUser blockedUser, TextConverter textConverter) {
        blockedUser.setBlockedOperation(blockedOperation);
        blockedUser.setRemoteNodeName(nodeName);
        blockedUser.setEntryNodeName(entryNodeName);
        blockedUser.setEntryPostingId(entryPostingId);
        blockedUser.setDeadline(Util.toTimestamp(deadline));
        blockedUser.setReasonSrc(reasonSrc != null ? reasonSrc : "");
        blockedUser.setReasonSrcFormat(reasonSrcFormat);
        if (!ObjectUtils.isEmpty(reasonSrc) && reasonSrcFormat != SourceFormat.APPLICATION) {
            blockedUser.setReason(textConverter.toHtml(reasonSrcFormat, reasonSrc));
        } else {
            blockedUser.setReason(blockedUser.getReasonSrc());
        }
    }

}
