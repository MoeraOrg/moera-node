package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarteVerificationInfo {

    private boolean valid;
    private String clientName;
    private List<String> clientScope;
    private List<String> adminScope;
    private String errorCode;
    private String errorMessage;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<String> getClientScope() {
        return clientScope;
    }

    public void setClientScope(List<String> clientScope) {
        this.clientScope = clientScope;
    }

    public List<String> getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(List<String> adminScope) {
        this.adminScope = adminScope;
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
