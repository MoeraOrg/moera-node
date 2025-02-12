package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.StorySummaryBlocked;

public class StorySummaryBlockedUtil {

    public static StorySummaryBlocked build(Collection<BlockedOperation> operations, Long period) {
        StorySummaryBlocked storySummaryBlocked = new StorySummaryBlocked();
        storySummaryBlocked.setOperations(new ArrayList<>(operations));
        storySummaryBlocked.setPeriod(period);
        return storySummaryBlocked;
    }

}
