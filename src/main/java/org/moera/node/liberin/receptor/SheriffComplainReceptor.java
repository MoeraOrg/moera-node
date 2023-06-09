package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainRepository;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.instant.SheriffInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteSheriffComplainDecidedLiberin;
import org.moera.node.liberin.model.SheriffComplainAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupUpdatedLiberin;
import org.moera.node.mail.ComplainAddedMail;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.event.SheriffComplainAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupAddedEvent;
import org.moera.node.model.event.SheriffComplainGroupUpdatedEvent;
import org.moera.node.model.notification.SheriffComplainDecidedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class SheriffComplainReceptor extends LiberinReceptorBase {

    @Inject
    private SheriffComplainRepository sheriffComplainRepository;

    @Inject
    private SheriffInstants sheriffInstants;

    @LiberinMapping
    public void groupAdded(SheriffComplainGroupAddedLiberin liberin) {
        send(liberin, new SheriffComplainGroupAddedEvent(liberin.getGroup()));
    }

    @LiberinMapping
    public void groupUpdated(SheriffComplainGroupUpdatedLiberin liberin) {
        send(liberin, new SheriffComplainGroupUpdatedEvent(liberin.getGroup()));
        if (liberin.getGroup().getStatus() != liberin.getPrevStatus()) {
            switch (liberin.getGroup().getStatus()) {
                case POSTED:
                    break;
                case PREPARED:
                    sheriffInstants.complainAdded(universalContext.nodeName(),
                            new AvatarImage(universalContext.getAvatar()), liberin.getGroup().getId().toString());
                    send(new ComplainAddedMail(liberin.getGroup().getId()));
                    break;
                default:
                    sheriffComplainRepository.findByGroupId(universalContext.nodeId(), liberin.getGroup().getId())
                            .forEach(complain -> notifyDecided(complain.getOwnerName(), liberin.getGroup()));
            }
        }
    }

    @LiberinMapping
    public void added(SheriffComplainAddedLiberin liberin) {
        SheriffComplain complain = liberin.getComplain();
        SheriffComplainGroup group = liberin.getGroup();
        send(liberin, new SheriffComplainAddedEvent(complain, group.getId()));
        if (group.getStatus() != SheriffComplainStatus.POSTED && group.getStatus() != SheriffComplainStatus.PREPARED) {
            notifyDecided(complain.getOwnerName(), group);
        }
    }

    private void notifyDecided(String targetNodeName, SheriffComplainGroup group) {
        send(Directions.single(universalContext.nodeId(), targetNodeName),
                new SheriffComplainDecidedNotification(group.getRemoteNodeName(), group.getRemoteFeedName(),
                        group.getRemotePostingOwnerName(), group.getRemotePostingOwnerFullName(),
                        group.getRemotePostingHeading(), group.getRemotePostingId(), group.getRemoteCommentOwnerName(),
                        group.getRemoteCommentOwnerFullName(), group.getRemotePostingHeading(),
                        group.getRemoteCommentId(), group.getId().toString()));
    }

    @LiberinMapping
    public void remoteDecided(RemoteSheriffComplainDecidedLiberin liberin) {
        sheriffInstants.complainDecided(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                liberin.getPostingOwnerName(), liberin.getPostingOwnerFullName(), liberin.getPostingHeading(),
                liberin.getPostingId(), liberin.getCommentOwnerName(), liberin.getCommentOwnerFullName(),
                liberin.getCommentHeading(), liberin.getCommentId(), liberin.getSheriffName(),
                liberin.getSheriffAvatar(), liberin.getComplainGroupId());
    }

}
