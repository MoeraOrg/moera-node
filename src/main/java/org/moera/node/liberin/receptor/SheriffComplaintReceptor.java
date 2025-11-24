package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintRepository;
import org.moera.node.instant.SheriffInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteSheriffComplaintDecidedLiberin;
import org.moera.node.liberin.model.SheriffComplaintAddedLiberin;
import org.moera.node.liberin.model.SheriffComplaintGroupAddedLiberin;
import org.moera.node.liberin.model.SheriffComplaintGroupUpdatedLiberin;
import org.moera.node.mail.ComplaintAddedMail;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.event.SheriffComplaintAddedEvent;
import org.moera.node.model.event.SheriffComplaintGroupAddedEvent;
import org.moera.node.model.event.SheriffComplaintGroupUpdatedEvent;
import org.moera.node.model.notification.SheriffComplaintDecidedNotificationUtil;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class SheriffComplaintReceptor extends LiberinReceptorBase {

    @Inject
    private SheriffComplaintRepository sheriffComplaintRepository;

    @Inject
    private SheriffInstants sheriffInstants;

    @LiberinMapping
    public void groupAdded(SheriffComplaintGroupAddedLiberin liberin) {
        send(liberin, new SheriffComplaintGroupAddedEvent(liberin.getGroup()));
    }

    @LiberinMapping
    public void groupUpdated(SheriffComplaintGroupUpdatedLiberin liberin) {
        send(liberin, new SheriffComplaintGroupUpdatedEvent(liberin.getGroup()));
        if (liberin.getGroup().getStatus() != liberin.getPrevStatus()) {
            switch (liberin.getGroup().getStatus()) {
                case POSTED:
                    break;
                case PREPARED:
                    sheriffInstants.complaintAdded(
                        universalContext.nodeName(),
                        universalContext.getAvatar() != null
                            ? AvatarImageUtil.build(universalContext.getAvatar())
                            : null,
                        liberin.getGroup().getId().toString()
                    );
                    send(new ComplaintAddedMail(liberin.getGroup().getId()));
                    break;
                default:
                    sheriffComplaintRepository.findByGroupId(universalContext.nodeId(), liberin.getGroup().getId())
                        .forEach(complaint -> notifyDecided(complaint.getOwnerName(), liberin.getGroup()));
            }
        }
    }

    @LiberinMapping
    public void added(SheriffComplaintAddedLiberin liberin) {
        SheriffComplaint complaint = liberin.getComplaint();
        SheriffComplaintGroup group = liberin.getGroup();
        send(liberin, new SheriffComplaintAddedEvent(complaint, group.getId()));
        if (
            group.getStatus() != SheriffComplaintStatus.POSTED
            && group.getStatus() != SheriffComplaintStatus.PREPARED
        ) {
            notifyDecided(complaint.getOwnerName(), group);
        }
    }

    private void notifyDecided(String targetNodeName, SheriffComplaintGroup group) {
        send(
            Directions.single(universalContext.nodeId(), targetNodeName),
            SheriffComplaintDecidedNotificationUtil.build(
                group.getRemoteNodeName(),
                group.getRemoteFeedName(),
                group.getRemotePostingOwnerName(),
                group.getRemotePostingOwnerFullName(),
                group.getRemotePostingHeading(),
                group.getRemotePostingId(),
                group.getRemoteCommentOwnerName(),
                group.getRemoteCommentOwnerFullName(),
                group.getRemotePostingHeading(),
                group.getRemoteCommentId(),
                group.getId().toString()
            )
        );
    }

    @LiberinMapping
    public void remoteDecided(RemoteSheriffComplaintDecidedLiberin liberin) {
        sheriffInstants.complaintDecided(
            liberin.getRemoteNodeName(),
            liberin.getRemoteFeedName(),
            liberin.getPostingOwnerName(),
            liberin.getPostingOwnerFullName(),
            liberin.getPostingHeading(),
            liberin.getPostingId(),
            liberin.getCommentOwnerName(),
            liberin.getCommentOwnerFullName(),
            liberin.getCommentHeading(),
            liberin.getCommentId(),
            liberin.getSheriffName(),
            liberin.getSheriffAvatar(),
            liberin.getComplaintGroupId()
        );
    }

}
