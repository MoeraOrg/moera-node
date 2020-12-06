package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.naming.rpc.OperationStatus;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhoAmI {

    private String nodeName;
    private boolean nodeNameChanging;

    public WhoAmI() {
    }

    public WhoAmI(Options options) {
        nodeName = options.nodeName();
        OperationStatus status = OperationStatus.forValue(options.getString("naming.operation.status"));
        nodeNameChanging = status == OperationStatus.WAITING
                || status == OperationStatus.ADDED
                || status == OperationStatus.STARTED;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isNodeNameChanging() {
        return nodeNameChanging;
    }

    public void setNodeNameChanging(boolean nodeNameChanging) {
        this.nodeNameChanging = nodeNameChanging;
    }

}
