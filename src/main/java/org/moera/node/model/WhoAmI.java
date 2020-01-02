package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhoAmI {

    private String nodeName;

    public WhoAmI() {
    }

    public WhoAmI(Options options) {
        nodeName = options.nodeName();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

}
