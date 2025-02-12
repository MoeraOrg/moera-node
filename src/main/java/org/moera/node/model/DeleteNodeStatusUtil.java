package org.moera.node.model;

import org.moera.lib.node.types.DeleteNodeStatus;
import org.moera.node.option.Options;

public class DeleteNodeStatusUtil {

    public static DeleteNodeStatus build(Options options) {
        DeleteNodeStatus status = new DeleteNodeStatus();
        status.setRequested(options.getBool("delete-node.requested"));
        return status;
    }

}
