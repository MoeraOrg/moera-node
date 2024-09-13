package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SearchEngineClickedLiberin;
import org.moera.node.model.notification.SearchEngineClickedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class SearchEngineReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void clicked(SearchEngineClickedLiberin liberin) {
        send(Directions.single(liberin.getNodeId(), liberin.getClick().getOwnerName()),
                new SearchEngineClickedNotification(liberin.getClick()));
    }

}
