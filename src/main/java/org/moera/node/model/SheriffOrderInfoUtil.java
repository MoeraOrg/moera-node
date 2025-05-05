package org.moera.node.model;

import org.moera.lib.node.types.SheriffOrderInfo;
import org.moera.node.data.SheriffOrder;
import org.moera.node.util.Util;

public class SheriffOrderInfoUtil {

    public static SheriffOrderInfo build(SheriffOrder sheriffOrder, String sheriffName) {
        SheriffOrderInfo info = new SheriffOrderInfo();
        info.setId(sheriffOrder.getId().toString());
        info.setDelete(sheriffOrder.isDelete());
        info.setSheriffName(sheriffName);
        info.setNodeName(sheriffOrder.getRemoteNodeName());
        info.setNodeFullName(sheriffOrder.getRemoteNodeFullName());
        info.setFeedName(sheriffOrder.getRemoteFeedName());
        info.setPostingOwnerName(sheriffOrder.getRemotePostingOwnerName());
        info.setPostingOwnerFullName(sheriffOrder.getRemotePostingOwnerFullName());
        info.setPostingOwnerGender(sheriffOrder.getRemotePostingOwnerGender());
        info.setPostingHeading(sheriffOrder.getRemotePostingHeading());
        info.setPostingId(sheriffOrder.getRemotePostingId());
        info.setPostingRevisionId(sheriffOrder.getRemotePostingRevisionId());
        info.setCommentOwnerName(sheriffOrder.getRemoteCommentOwnerName());
        info.setCommentOwnerFullName(sheriffOrder.getRemoteCommentOwnerFullName());
        info.setCommentOwnerGender(sheriffOrder.getRemoteCommentOwnerGender());
        info.setCommentHeading(sheriffOrder.getRemoteCommentHeading());
        info.setCommentId(sheriffOrder.getRemoteCommentId());
        info.setCommentRevisionId(sheriffOrder.getRemoteCommentRevisionId());
        info.setCategory(sheriffOrder.getCategory());
        info.setReasonCode(sheriffOrder.getReasonCode());
        info.setReasonDetails(sheriffOrder.getReasonDetails());
        info.setCreatedAt(Util.toEpochSecond(sheriffOrder.getCreatedAt()));
        info.setMoment(sheriffOrder.getMoment());
        info.setSignature(sheriffOrder.getSignature());
        info.setSignatureVersion(sheriffOrder.getSignatureVersion());
        if (sheriffOrder.getComplaintGroup() != null) {
            info.setComplaintGroupId(sheriffOrder.getComplaintGroup().getId().toString());
        }
        return info;
    }

}
