package org.moera.node.api.node;

import org.moera.lib.node.exception.MoeraNodeException;

public class MoeraNodeConcurrencyException extends MoeraNodeException {

    public MoeraNodeConcurrencyException(String message) {
        super(message);
    }

    public MoeraNodeConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MoeraNodeConcurrencyException(Throwable cause) {
        super(cause);
    }

}
