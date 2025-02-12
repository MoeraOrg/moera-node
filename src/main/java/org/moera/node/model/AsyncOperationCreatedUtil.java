package org.moera.node.model;

import java.util.UUID;

import org.moera.lib.node.types.AsyncOperationCreated;

public class AsyncOperationCreatedUtil {

    public static AsyncOperationCreated build(UUID id) {
        AsyncOperationCreated asyncOperationCreated = new AsyncOperationCreated();
        asyncOperationCreated.setId(id.toString());
        return asyncOperationCreated;
    }

}
