package org.moera.node.liberin.receptor;

import java.util.UUID;

import org.moera.node.auth.principal.Principal;
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

@LiberinReceptor
public class FriendGroupReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(FriendGroupAddedLiberin liberin) {
        FriendGroupInfo friendGroupInfo = new FriendGroupInfo(liberin.getFriendGroup());
        send(liberin, new FriendGroupAddedEvent(friendGroupInfo, visibilityFilter(liberin.getFriendGroup())));
    }

    @LiberinMapping
    public void updated(FriendGroupUpdatedLiberin liberin) {
        FriendGroupInfo friendGroupInfo = new FriendGroupInfo(liberin.getFriendGroup());
        Principal filter = visibilityFilter(liberin.getFriendGroup());
        Principal latestFilter = visibilityFilter(liberin.getFriendGroup().getId(), liberin.getLatestViewPrincipal());
        send(liberin, new FriendGroupAddedEvent(friendGroupInfo, filter.a().andNot(latestFilter)));
        send(liberin, new FriendGroupUpdatedEvent(friendGroupInfo, filter.a().and(latestFilter)));
        send(liberin, new FriendGroupDeletedEvent(friendGroupInfo.getId(), filter.not().and(latestFilter)));
    }

    @LiberinMapping
    public void deleted(FriendGroupDeletedLiberin liberin) {
        Principal latestFilter = visibilityFilter(liberin.getFriendGroupId(), liberin.getLatestViewPrincipal());
        send(liberin, new FriendGroupDeletedEvent(liberin.getFriendGroupId().toString(), latestFilter));
    }

    private Principal visibilityFilter(FriendGroup friendGroup) {
        return visibilityFilter(friendGroup.getId(), friendGroup.getViewPrincipal());
    }

    private Principal visibilityFilter(UUID friendGroupId, Principal viewPrincipal) {
        if (viewPrincipal.isPublic()) {
            return Principal.PUBLIC;
        } else if (viewPrincipal.isPrivate()) {
            return Principal.ofFriendGroup(friendGroupId.toString());
        } else {
            return Principal.ADMIN;
        }
    }

}
