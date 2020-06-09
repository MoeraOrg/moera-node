package org.moera.node.api;

import java.net.URI;

public class NodeApiNotFoundException extends NodeApiErrorStatusException {

    public NodeApiNotFoundException(URI uri) {
        super("Object not found: " + uri);
    }

}
