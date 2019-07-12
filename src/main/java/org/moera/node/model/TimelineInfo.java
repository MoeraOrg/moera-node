package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

public class TimelineInfo {

    private Map<String, String[]> operations;

    public TimelineInfo() {
        operations = Collections.singletonMap("add", new String[]{"admin"});
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
