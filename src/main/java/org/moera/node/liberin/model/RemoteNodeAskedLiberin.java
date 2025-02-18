package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.AskDescription;
import org.moera.node.liberin.Liberin;

public class RemoteNodeAskedLiberin extends Liberin {

    private String nodeName;
    private AskDescription askDescription;

    public RemoteNodeAskedLiberin(String nodeName, AskDescription askDescription) {
        this.nodeName = nodeName;
        this.askDescription = askDescription;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public AskDescription getAskDescription() {
        return askDescription;
    }

    public void setAskDescription(AskDescription askDescription) {
        this.askDescription = askDescription;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("askDescription", askDescription);
    }

}
