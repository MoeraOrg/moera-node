package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.node.instant.NodeInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.DefrostLiberin;

@LiberinReceptor
public class NodeReceptor extends LiberinReceptorBase {

    @Inject
    private NodeInstants nodeInstants;

    @LiberinMapping
    public void defrost(DefrostLiberin liberin) {
        nodeInstants.defrosting();
    }

}
