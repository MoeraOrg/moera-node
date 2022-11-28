package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.instant.FriendInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFromFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteToFriendGroupAddedLiberin;
import org.springframework.util.ObjectUtils;

@LiberinReceptor
public class RemoteFriendGroupReceptor extends LiberinReceptorBase {

    @Inject
    private FriendInstants friendInstants;

    @LiberinMapping
    public void toGroupAdded(RemoteToFriendGroupAddedLiberin liberin) {
        if (!ObjectUtils.isEmpty(liberin.getFriendOf().getRemoteGroupTitle())) {
            friendInstants.added(liberin.getFriendOf());
        }
    }

    @LiberinMapping
    public void fromGroupDeleted(RemoteFromFriendGroupDeletedLiberin liberin) {
        if (!ObjectUtils.isEmpty(liberin.getFriendOf().getRemoteGroupTitle())) {
            friendInstants.deleted(liberin.getFriendOf());
        }
    }

    @LiberinMapping
    public void groupDeleted(RemoteFriendGroupDeletedLiberin liberin) {
        if (!ObjectUtils.isEmpty(liberin.getFriendOf().getRemoteGroupTitle())) {
            friendInstants.groupDeleted(liberin.getFriendOf());
        }
    }

}
