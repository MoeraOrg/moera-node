package org.moera.node.task;

public class CallApiUnknownNameException extends CallApiException {

    public CallApiUnknownNameException(String nodeName) {
        super(String.format("Node name '%s' not found", nodeName));
    }

}
