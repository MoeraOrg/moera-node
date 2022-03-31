package org.moera.node.liberin.receptor;

import java.util.Objects;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.ProfileUpdatedLiberin;
import org.moera.node.mail.EmailConfirmMail;
import org.moera.node.model.event.NodeNameChangedEvent;
import org.moera.node.model.event.ProfileUpdatedEvent;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class ProfileReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(ProfileUpdatedLiberin liberin) {
        eventManager.send(liberin.getNodeId(), liberin.getClientId(), new ProfileUpdatedEvent());
        eventManager.send(liberin.getNodeId(), liberin.getClientId(),
                new NodeNameChangedEvent(liberin.getNodeName(), liberin.getOptions(), liberin.getAvatar()));
        notificationSenderPool.send(Directions.profileSubscribers(liberin.getNodeId()),
                new ProfileUpdatedNotification());
        if (!Objects.equals(liberin.getOptions().getString("profile.email"), liberin.getOldEmail())) {
            mailService.send(liberin.getNodeId(), new EmailConfirmMail());
        }
    }

}
