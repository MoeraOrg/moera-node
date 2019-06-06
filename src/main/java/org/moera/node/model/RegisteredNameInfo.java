package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import org.moera.naming.rpc.OperationStatus;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;

public class RegisteredNameInfo {

    private String name;
    private Integer generation;
    private String operationStatus;
    private Long operationStatusUpdated;
    private String operationErrorCode;
    private String operationErrorMessage;
    private Map<String, String[]> operations;

    public RegisteredNameInfo() {
    }

    public RegisteredNameInfo(RequestContext requestContext) {
        Options options = requestContext.getOptions();
        name = options.getString("profile.registered-name");
        generation = options.getInt("profile.registered-name.generation");
        if (requestContext.isAdmin()) {
            operationStatus = options.getString("naming.operation.status");
            OperationStatus status = OperationStatus.forValue(operationStatus);
            if (status != null) {
                switch (status) {
                    case WAITING:
                        break;
                    case ADDED:
                        operationStatusUpdated = options.getLong("naming.operation.added");
                        break;
                    case STARTED:
                        operationStatusUpdated = options.getLong("naming.operation.status.updated");
                        break;
                    case SUCCEEDED:
                        operationStatusUpdated = options.getLong("naming.operation.completed");
                        break;
                    case FAILED:
                    case UNKNOWN:
                        operationStatusUpdated = options.getLong("naming.operation.completed");
                        operationErrorCode = options.getString("naming.operation.error-code");
                        operationErrorMessage = options.getString("naming.operation.error-message");
                        break;
                }
            }
        }
        operations = Collections.singletonMap("manage", new String[]{"admin"});
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public Long getOperationStatusUpdated() {
        return operationStatusUpdated;
    }

    public void setOperationStatusUpdated(Long operationStatusUpdated) {
        this.operationStatusUpdated = operationStatusUpdated;
    }

    public String getOperationErrorCode() {
        return operationErrorCode;
    }

    public void setOperationErrorCode(String operationErrorCode) {
        this.operationErrorCode = operationErrorCode;
    }

    public String getOperationErrorMessage() {
        return operationErrorMessage;
    }

    public void setOperationErrorMessage(String operationErrorMessage) {
        this.operationErrorMessage = operationErrorMessage;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
