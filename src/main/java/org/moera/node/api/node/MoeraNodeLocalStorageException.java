package org.moera.node.api.node;

import org.moera.lib.node.exception.MoeraNodeException;

public class MoeraNodeLocalStorageException extends MoeraNodeException {

    public MoeraNodeLocalStorageException(String message) {
        super(message);
    }

    public MoeraNodeLocalStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MoeraNodeLocalStorageException(Throwable cause) {
        super(cause);
    }

}
