package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.AvatarAddedLiberin;
import org.moera.node.liberin.model.AvatarDeletedLiberin;
import org.moera.node.liberin.model.AvatarOrderedLiberin;
import org.moera.node.model.event.AvatarAddedEvent;
import org.moera.node.model.event.AvatarDeletedEvent;
import org.moera.node.model.event.AvatarOrderedEvent;

@LiberinReceptor
public class AvatarReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(AvatarAddedLiberin liberin) {
        send(liberin, new AvatarAddedEvent(liberin.getAvatar()));
    }

    @LiberinMapping
    public void ordered(AvatarOrderedLiberin liberin) {
        send(liberin, new AvatarOrderedEvent(liberin.getAvatar()));
    }

    @LiberinMapping
    public void deleted(AvatarDeletedLiberin liberin) {
        send(liberin, new AvatarDeletedEvent(liberin.getAvatar()));
    }

}
