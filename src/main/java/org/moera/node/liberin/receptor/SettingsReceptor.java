package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FeaturesUpdatedLiberin;
import org.moera.node.liberin.model.NodeSettingsMetadataChangedLiberin;
import org.moera.node.liberin.model.SettingsChangedLiberin;
import org.moera.node.model.event.ClientSettingsChangedEvent;
import org.moera.node.model.event.FeaturesUpdatedEvent;
import org.moera.node.model.event.NodeSettingsChangedEvent;
import org.moera.node.model.event.NodeSettingsMetaChangedEvent;

@LiberinReceptor
public class SettingsReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void nodeMetadataChanged(NodeSettingsMetadataChangedLiberin liberin) {
        send(liberin, new NodeSettingsMetaChangedEvent());
        send(liberin, new NodeSettingsChangedEvent());
    }

    @LiberinMapping
    public void changed(SettingsChangedLiberin liberin) {
        if (liberin.isNodeChanged()) {
            send(liberin, new NodeSettingsChangedEvent());
        }
        if (liberin.isClientChanged()) {
            send(liberin, new ClientSettingsChangedEvent());
        }
    }

    @LiberinMapping
    public void featuresUpdated(FeaturesUpdatedLiberin liberin) {
        send(liberin, new FeaturesUpdatedEvent(liberin.getFeatures()));
    }

}
