package org.moera.node.liberin.receptor;

import java.util.UUID;

import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.node.data.FriendGroup;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FriendGroupAddedLiberin;
import org.moera.node.liberin.model.FriendGroupDeletedLiberin;
import org.moera.node.liberin.model.FriendGroupUpdatedLiberin;
import org.moera.node.model.FriendGroupInfo;
import org.moera.node.model.event.FriendGroupAddedEvent;
import org.moera.node.model.event.FriendGroupDeletedEvent;
import org.moera.node.model.event.FriendGroupUpdatedEvent;
import org.moera.node.model.notification.FriendGroupDeletedNotification;
import org.moera.node.model.notification.FriendGroupUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class FriendGroupReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(FriendGroupAddedLiberin liberin) {
        send(liberin, new FriendGroupAddedEvent(new FriendGroupInfo(liberin.getFriendGroup(), true),
                Principal.ADMIN));
        send(liberin, new FriendGroupAddedEvent(new FriendGroupInfo(liberin.getFriendGroup(), false),
                visibilityFilter(liberin.getFriendGroup()).a().andNot(Principal.ADMIN)));
    }

    @LiberinMapping
    public void updated(FriendGroupUpdatedLiberin liberin) {
        FriendGroupInfo friendGroupInfo = new FriendGroupInfo(liberin.getFriendGroup(), true);
        send(liberin, new FriendGroupUpdatedEvent(friendGroupInfo, Principal.ADMIN));

        friendGroupInfo = new FriendGroupInfo(liberin.getFriendGroup(), false);
        Principal filter = visibilityFilter(liberin.getFriendGroup());
        Principal latestFilter = visibilityFilter(liberin.getFriendGroup().getId(), liberin.getLatestViewPrincipal());

        send(liberin, new FriendGroupAddedEvent(friendGroupInfo, filter.a().andNot(latestFilter)));

        PrincipalExpression updateFilter = filter.a().and(latestFilter).andNot(Principal.ADMIN);
        send(Directions.friends(liberin.getNodeId(), liberin.getFriendGroup().getId(), updateFilter),
                new FriendGroupUpdatedNotification(friendGroupInfo));
        send(liberin, new FriendGroupUpdatedEvent(friendGroupInfo, updateFilter));

        send(liberin, new FriendGroupDeletedEvent(friendGroupInfo.getId(), filter.not().and(latestFilter)));
    }

    @LiberinMapping
    public void deleted(FriendGroupDeletedLiberin liberin) {
        if (liberin.getFriendName() == null) {
            send(liberin, new FriendGroupDeletedEvent(liberin.getFriendGroupId().toString(), Principal.ADMIN));
        } else if (!liberin.getLatestViewPrincipal().isAdmin()) {
            send(Directions.single(liberin.getNodeId(), liberin.getFriendName()),
                    new FriendGroupDeletedNotification(liberin.getFriendGroupId()));
            send(liberin, new FriendGroupDeletedEvent(liberin.getFriendGroupId().toString(),
                    Principal.ofNode(liberin.getFriendName())));
        }
    }

    private Principal visibilityFilter(FriendGroup friendGroup) {
        return visibilityFilter(friendGroup.getId(), friendGroup.getViewPrincipal());
    }

    private Principal visibilityFilter(UUID friendGroupId, Principal viewPrincipal) {
        return viewPrincipal.isPublic() ? Principal.PUBLIC : Principal.ofFriendGroup(friendGroupId.toString());
    }

}
