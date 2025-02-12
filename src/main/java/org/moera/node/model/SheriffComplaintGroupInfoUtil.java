package org.moera.node.model;

import org.moera.lib.node.types.SheriffComplaintGroupInfo;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.util.Util;

public class SheriffComplaintGroupInfoUtil {
    
    public static SheriffComplaintGroupInfo build(SheriffComplaintGroup sheriffComplaintGroup) {
        SheriffComplaintGroupInfo info = new SheriffComplaintGroupInfo();
        info.setId(sheriffComplaintGroup.getId().toString());
        info.setRemoteNodeName(sheriffComplaintGroup.getRemoteNodeName());
        info.setRemoteNodeFullName(sheriffComplaintGroup.getRemoteNodeFullName());
        info.setRemoteFeedName(sheriffComplaintGroup.getRemoteFeedName());
        info.setRemotePostingOwnerName(sheriffComplaintGroup.getRemotePostingOwnerName());
        info.setRemotePostingOwnerFullName(sheriffComplaintGroup.getRemotePostingOwnerFullName());
        info.setRemotePostingOwnerGender(sheriffComplaintGroup.getRemotePostingOwnerGender());
        info.setRemotePostingHeading(sheriffComplaintGroup.getRemotePostingHeading());
        info.setRemotePostingId(sheriffComplaintGroup.getRemotePostingId());
        info.setRemotePostingRevisionId(sheriffComplaintGroup.getRemotePostingRevisionId());
        info.setRemoteCommentOwnerName(sheriffComplaintGroup.getRemoteCommentOwnerName());
        info.setRemoteCommentOwnerFullName(sheriffComplaintGroup.getRemoteCommentOwnerFullName());
        info.setRemoteCommentOwnerGender(sheriffComplaintGroup.getRemoteCommentOwnerGender());
        info.setRemoteCommentHeading(sheriffComplaintGroup.getRemoteCommentHeading());
        info.setRemoteCommentId(sheriffComplaintGroup.getRemoteCommentId());
        info.setRemoteCommentRevisionId(sheriffComplaintGroup.getRemoteCommentRevisionId());
        info.setCreatedAt(Util.toEpochSecond(sheriffComplaintGroup.getCreatedAt()));
        info.setMoment(sheriffComplaintGroup.getMoment());
        info.setStatus(sheriffComplaintGroup.getStatus());
        info.setDecisionCode(sheriffComplaintGroup.getDecisionCode());
        info.setDecisionDetails(sheriffComplaintGroup.getDecisionDetails());
        info.setDecidedAt(Util.toEpochSecond(sheriffComplaintGroup.getDecidedAt()));
        info.setAnonymous(sheriffComplaintGroup.isAnonymous());
        return info;
    }

}
