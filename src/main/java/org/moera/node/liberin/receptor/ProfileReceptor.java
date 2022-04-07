package org.moera.node.liberin.receptor;

import java.util.Objects;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.liberin.model.ProfileUpdatedLiberin;
import org.moera.node.liberin.model.RegisteredNameOperationStatusLiberin;
import org.moera.node.mail.EmailConfirmMail;
import org.moera.node.model.event.NodeNameChangedEvent;
import org.moera.node.model.event.ProfileUpdatedEvent;
import org.moera.node.model.event.RegisteredNameOperationStatusEvent;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class ProfileReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(ProfileUpdatedLiberin liberin) {
        send(liberin, new ProfileUpdatedEvent());
        send(liberin, new NodeNameChangedEvent(liberin.getNodeName(), liberin.getOptions(), liberin.getAvatar()));
        send(Directions.profileSubscribers(liberin.getNodeId()), new ProfileUpdatedNotification());
        if (!Objects.equals(liberin.getOptions().getString("profile.email"), liberin.getOldEmail())) {
            send(liberin, new EmailConfirmMail());
        }
    }

    @LiberinMapping
    public void nodeNameChanged(NodeNameChangedLiberin liberin) {
        send(liberin, new NodeNameChangedEvent("", liberin.getOptions(), liberin.getAvatar()));
    }

    @LiberinMapping
    public void registeredNameOperationStatus(RegisteredNameOperationStatusLiberin liberin) {
        send(liberin, new RegisteredNameOperationStatusEvent());
    }

}
