package org.moera.node.liberin.receptor;

import java.util.Objects;

import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.notifications.ProfileUpdatedNotification;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.liberin.model.ProfileUpdatedLiberin;
import org.moera.node.liberin.model.RegisteredNameOperationStatusLiberin;
import org.moera.node.mail.DomainCreatedMail;
import org.moera.node.mail.EmailConfirmMail;
import org.moera.node.model.event.NodeNameChangedEvent;
import org.moera.node.model.event.ProfileUpdatedEvent;
import org.moera.node.model.event.RegisteredNameOperationStatusEvent;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.springframework.util.ObjectUtils;

@LiberinReceptor
public class ProfileReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void updated(ProfileUpdatedLiberin liberin) {
        send(liberin, new ProfileUpdatedEvent());
        send(liberin, new NodeNameChangedEvent(liberin.getNodeName(), liberin.getOptions(), liberin.getAvatar()));
        send(Directions.profileSubscribers(liberin.getNodeId()), new ProfileUpdatedNotification());
        send(
            Directions.searchSubscribers(liberin.getNodeId()),
            SearchContentUpdatedNotificationUtil.build(SearchContentUpdateType.PROFILE)
        );
        if (!Objects.equals(liberin.getOptions().getString("profile.email"), liberin.getPrevEmail())) {
            send(new EmailConfirmMail());
        }
    }

    @LiberinMapping
    public void nodeNameChanged(NodeNameChangedLiberin liberin) {
        send(liberin, new NodeNameChangedEvent(liberin.getNodeName(), liberin.getOptions(), liberin.getAvatar()));
        if (ObjectUtils.isEmpty(liberin.getPrevNodeName())) {
            sendToRoot(new DomainCreatedMail(liberin.getNodeName()));
        }
    }

    @LiberinMapping
    public void registeredNameOperationStatus(RegisteredNameOperationStatusLiberin liberin) {
        send(liberin, new RegisteredNameOperationStatusEvent());
    }

}
