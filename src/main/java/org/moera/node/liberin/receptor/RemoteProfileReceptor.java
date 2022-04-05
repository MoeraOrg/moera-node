package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteNodeAvatarChangedLiberin;
import org.moera.node.liberin.model.RemoteNodeFullNameChangedLiberin;
import org.moera.node.model.event.RemoteNodeAvatarChangedEvent;
import org.moera.node.model.event.RemoteNodeFullNameChangedEvent;

@LiberinReceptor
public class RemoteProfileReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void fullNameChanged(RemoteNodeFullNameChangedLiberin liberin) {
        send(liberin, new RemoteNodeFullNameChangedEvent(liberin.getNodeName(), liberin.getFullName()));
    }

    @LiberinMapping
    public void avatarChanged(RemoteNodeAvatarChangedLiberin liberin) {
        send(liberin, new RemoteNodeAvatarChangedEvent(liberin.getNodeName(), liberin.getAvatar()));
    }

}
