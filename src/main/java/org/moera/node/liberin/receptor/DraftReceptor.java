package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.DraftAddedLiberin;
import org.moera.node.liberin.model.DraftDeletedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.model.event.DraftAddedEvent;
import org.moera.node.model.event.DraftDeletedEvent;
import org.moera.node.model.event.DraftUpdatedEvent;

@LiberinReceptor
public class DraftReceptor extends LiberinReceptorBase {

    public void added(DraftAddedLiberin liberin) {
        send(liberin, new DraftAddedEvent(liberin.getDraft()));
    }

    public void updated(DraftUpdatedLiberin liberin) {
        send(liberin, new DraftUpdatedEvent(liberin.getDraft()));
    }

    public void deleted(DraftDeletedLiberin liberin) {
        send(liberin, new DraftDeletedEvent(liberin.getDraft()));
    }

}
