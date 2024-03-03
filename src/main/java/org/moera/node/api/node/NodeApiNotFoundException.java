package org.moera.node.api.node;

import java.net.URI;

public class NodeApiNotFoundException extends NodeApiErrorStatusException {

    public NodeApiNotFoundException(URI uri) {
        super("Object not found: " + uri);
    }

}
