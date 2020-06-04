package org.moera.node.task;

import java.net.URI;

public class CallApiNotFoundException extends CallApiException {

    public CallApiNotFoundException(URI uri) {
        super("Object not found: " + uri);
    }

}
