package org.moera.node.model;

import org.moera.lib.naming.types.OperationStatus;
import org.moera.lib.node.types.NodeNameInfo;
import org.moera.lib.node.types.NodeNameOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;
import org.springframework.util.ObjectUtils;

public class NodeNameInfoUtil {
    
    public static NodeNameInfo build(RequestContext requestContext) {
        NodeNameInfo nodeNameInfo = new NodeNameInfo();
        
        Options options = requestContext.getOptions();
        if (options == null) {
            return nodeNameInfo;
        }
        
        nodeNameInfo.setName(options.nodeName());
        
        if (requestContext.isAdmin(Scope.NAME)) {
            String operationStatus = options.getString("naming.operation.status");
            nodeNameInfo.setOperationStatus(org.moera.lib.node.types.OperationStatus.parse(operationStatus));
            
            OperationStatus status = OperationStatus.forValue(operationStatus);
            if (status != null) {
                switch (status) {
                    case WAITING:
                        break;
                    case ADDED:
                        nodeNameInfo.setOperationStatusUpdated(options.getLong("naming.operation.added"));
                        break;
                    case STARTED:
                        nodeNameInfo.setOperationStatusUpdated(options.getLong("naming.operation.status.updated"));
                        break;
                    case SUCCEEDED:
                        nodeNameInfo.setOperationStatusUpdated(options.getLong("naming.operation.completed"));
                        break;
                    case FAILED:
                    case UNKNOWN:
                        nodeNameInfo.setOperationStatusUpdated(options.getLong("naming.operation.completed"));
                        nodeNameInfo.setOperationErrorCode(options.getString("naming.operation.error-code"));
                        nodeNameInfo.setOperationErrorMessage(options.getString("naming.operation.error-message"));
                        break;
                }
            }
            
            nodeNameInfo.setStoredMnemonic(!ObjectUtils.isEmpty(options.getString("profile.updating-key.mnemonic")));
        }

        NodeNameOperations operations = new NodeNameOperations();
        operations.setManage(Principal.ADMIN);
        nodeNameInfo.setOperations(operations);
        
        return nodeNameInfo;
    }

}
