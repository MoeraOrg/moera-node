package org.moera.node.liberin.receptor;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.node.data.FriendOf;
import org.moera.node.instant.FriendInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFriendshipUpdatedLiberin;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.FriendGroupDetailsUtil;
import org.moera.node.model.FriendOfInfoUtil;
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

        ContactInfo contactInfo = ContactInfoUtil.build(liberin.getContact(), universalContext.getOptions());
        List<FriendGroupDetails> groups = liberin.getCurrent().stream()
                .map(FriendGroupDetailsUtil::build)
                .collect(Collectors.toList());
        send(liberin, new RemoteFriendshipUpdatedEvent(
            FriendOfInfoUtil.build(contactInfo.getNodeName(), contactInfo, groups),
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
