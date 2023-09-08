package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.VerificationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemotePostingVerificationInfo {

    private String id;
    private String nodeName;
    private String postingId;
    private String revisionId;
    private VerificationStatus status;
    private String errorCode;
    private String errorMessage;

    public RemotePostingVerificationInfo() {
    }

    public RemotePostingVerificationInfo(RemotePostingVerification data) {
        id = data.getId().toString();
        nodeName = data.getNodeName();
        postingId = data.getPostingId();
        revisionId = data.getRevisionId();
        status = data.getStatus();
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

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
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

}
