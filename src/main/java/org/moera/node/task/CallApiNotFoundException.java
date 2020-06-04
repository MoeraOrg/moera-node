package org.moera.node.task;

import java.net.URI;

public class CallApiNotFoundException extends CallApiErrorStatusException {

    public CallApiNotFoundException(URI uri) {
        super("Object not found: " + uri);
    }

}
