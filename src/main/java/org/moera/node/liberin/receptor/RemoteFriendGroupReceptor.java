package org.moera.node.liberin.receptor;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.node.data.FriendOf;
import org.moera.node.instant.FriendInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFriendshipUpdatedLiberin;
import org.moera.node.model.ContactInfo;
import org.moera.node.model.FriendGroupDetails;
import org.moera.node.model.FriendOfInfo;
import org.moera.node.model.event.RemoteFriendshipUpdatedEvent;
import org.springframework.util.ObjectUtils;

@LiberinReceptor
public class RemoteFriendGroupReceptor extends LiberinReceptorBase {

    @Inject
    private FriendInstants friendInstants;

    @LiberinMapping
    public void updated(RemoteFriendshipUpdatedLiberin liberin) {
        liberin.getAdded().stream()
                .filter(fo -> !ObjectUtils.isEmpty(fo.getRemoteGroupTitle()))
                .forEach(fo -> friendInstants.added(fo));
        liberin.getDeleted().stream()
                .filter(fo -> !ObjectUtils.isEmpty(fo.getRemoteGroupTitle()))
                .forEach(fo -> friendInstants.deleted(fo));

        ContactInfo contactInfo = new ContactInfo(liberin.getContact(), universalContext.getOptions());
        List<FriendGroupDetails> groups = liberin.getCurrent().stream()
                .map(FriendGroupDetails::new)
                .collect(Collectors.toList());
        send(liberin, new RemoteFriendshipUpdatedEvent(
                new FriendOfInfo(contactInfo.getNodeName(), contactInfo, groups),
                FriendOf.getViewAllE(universalContext.getOptions())
        ));
    }

    @LiberinMapping
    public void groupDeleted(RemoteFriendGroupDeletedLiberin liberin) {
        if (!ObjectUtils.isEmpty(liberin.getFriendOf().getRemoteGroupTitle())) {
            friendInstants.groupDeleted(liberin.getFriendOf());
        }
    }

}
