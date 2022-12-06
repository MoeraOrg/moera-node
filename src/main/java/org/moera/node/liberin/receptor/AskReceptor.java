package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.AskInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.AskSubjectsChangedLiberin;
import org.moera.node.liberin.model.AskedToFriendLiberin;
import org.moera.node.liberin.model.AskedToSubscribeLiberin;
import org.moera.node.model.event.AskSubjectsChangedEvent;

@LiberinReceptor
public class AskReceptor extends LiberinReceptorBase {

    @Inject
    private AskInstants askInstants;

    @LiberinMapping
    public void askedToSubscribe(AskedToSubscribeLiberin liberin) {
        askInstants.askedToSubscribe(liberin.getRemoteNodeName(), liberin.getRemoteFullName(),
                liberin.getRemoteGender(), liberin.getRemoteAvatar(), liberin.getMessage());
    }

    @LiberinMapping
    public void askedToFriend(AskedToFriendLiberin liberin) {
        askInstants.askedToFriend(liberin.getRemoteNodeName(), liberin.getRemoteFullName(),
                liberin.getRemoteGender(), liberin.getRemoteAvatar(), liberin.getFriendGroupId(),
                liberin.getFriendGroupTitle(), liberin.getMessage());
    }

    @LiberinMapping
    public void subjectsChanged(AskSubjectsChangedLiberin liberin) {
        send(liberin, new AskSubjectsChangedEvent());
    }

}
