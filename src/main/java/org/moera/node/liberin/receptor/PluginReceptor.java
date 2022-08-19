package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PluginAddedLiberin;
import org.moera.node.liberin.model.PluginDeletedLiberin;
import org.moera.node.model.event.PluginsUpdatedEvent;

@LiberinReceptor
public class PluginReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(PluginAddedLiberin liberin) {
        send(liberin, new PluginsUpdatedEvent());
    }

    @LiberinMapping
    public void deleted(PluginDeletedLiberin liberin) {
        send(liberin, new PluginsUpdatedEvent());
    }

}
