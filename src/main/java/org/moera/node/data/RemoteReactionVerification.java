package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "remote_reaction_verifications")
public class RemoteReactionVerification {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String nodeName;

    @NotNull
    @Size(max = 40)
    private String postingId;

    @Size(max = 63)
    private String reactionOwnerName;

    @NotNull
    @Enumerated
    private VerificationStatus status = VerificationStatus.RUNNING;

    @Size(max = 63)
    private String errorCode;

    @Size(max = 255)
    private String errorMessage;

    @NotNull
    private Timestamp deadline;

    public RemoteReactionVerification() {
    }

    public RemoteReactionVerification(UUID nodeId, String nodeName, String postingId, String reactionOwnerName) {
        this.id = UUID.randomUUID();
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.reactionOwnerName = reactionOwnerName;
    }

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

    public String getReactionOwnerName() {
        return reactionOwnerName;
    }

    public void setReactionOwnerName(String reactionOwnerName) {
        this.reactionOwnerName = reactionOwnerName;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

}
