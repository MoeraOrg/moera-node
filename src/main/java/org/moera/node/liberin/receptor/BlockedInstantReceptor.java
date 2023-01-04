package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.BlockedInstantAddedLiberin;
import org.moera.node.liberin.model.BlockedInstantDeletedLiberin;
import org.moera.node.model.BlockedInstantInfo;
import org.moera.node.model.event.BlockedInstantAddedEvent;
import org.moera.node.model.event.BlockedInstantDeletedEvent;

@LiberinReceptor
public class BlockedInstantReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(BlockedInstantAddedLiberin liberin) {
        send(liberin, new BlockedInstantAddedEvent(new BlockedInstantInfo(liberin.getBlockedInstant())));
    }

    @LiberinMapping
    public void deleted(BlockedInstantDeletedLiberin liberin) {
        send(liberin, new BlockedInstantDeletedEvent(new BlockedInstantInfo(liberin.getBlockedInstant())));
    }

}
