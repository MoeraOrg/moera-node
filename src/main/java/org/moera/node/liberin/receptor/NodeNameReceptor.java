package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.model.event.NodeNameChangedEvent;

@LiberinReceptor
public class NodeNameReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void changed(NodeNameChangedLiberin liberin) {
        send(liberin, new NodeNameChangedEvent("", liberin.getOptions(), liberin.getAvatar()));
    }

}
