package org.moera.node.liberin.model;

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

}
