package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.RemoteReactionVerification;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteReactionVerificationInfo {

    private String id;
    private String nodeName;
    private String postingId;
    private String reactionOwnerName;
    private String status;
    private String errorCode;
    private String errorMessage;

    public RemoteReactionVerificationInfo() {
    }

    public RemoteReactionVerificationInfo(RemoteReactionVerification data) {
        id = data.getId().toString();
        nodeName = data.getNodeName();
        postingId = data.getPostingId();
        reactionOwnerName = data.getReactionOwnerName();
        status = data.getStatus().getValue();
        errorCode = data.getErrorCode();
        errorMessage = data.getErrorMessage();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

}
