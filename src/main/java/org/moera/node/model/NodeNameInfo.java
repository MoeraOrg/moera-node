package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.naming.rpc.OperationStatus;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeNameInfo {

    private String name;
    private String operationStatus;
    private Long operationStatusUpdated;
    private String operationErrorCode;
    private String operationErrorMessage;
    private Map<String, Principal> operations;

    public NodeNameInfo() {
    }

    public NodeNameInfo(RequestContext requestContext) {
        Options options = requestContext.getOptions();
        if (options == null) {
            return;
        }
        name = options.nodeName();
        if (requestContext.isAdmin(Scope.NAME)) {
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
        operations = Collections.singletonMap("manage", Principal.ADMIN);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
