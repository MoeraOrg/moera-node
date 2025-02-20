package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.SheriffOrderAttributes;
import org.moera.lib.node.types.SheriffOrderDetails;
import org.moera.node.data.SheriffOrder;
import org.moera.node.util.Util;

public class SheriffOrderDetailsUtil {

    public static SheriffOrderDetails build(
        String id, String sheriffName, AvatarDescription sheriffAvatar, SheriffOrderAttributes attributes
    ) {
        SheriffOrderDetails details = new SheriffOrderDetails();

        details.setId(id);
        details.setDelete(attributes.getDelete());
        details.setSheriffName(sheriffName);
        details.setSheriffAvatar(sheriffAvatar);
        details.setFeedName(attributes.getFeedName());
        details.setPostingId(attributes.getPostingId());
        details.setCommentId(attributes.getCommentId());
        details.setCategory(attributes.getCategory());
        details.setReasonCode(attributes.getReasonCode());
        details.setReasonDetails(attributes.getReasonDetails());

        return details;
    }

    public static void toSheriffOrder(SheriffOrderDetails details, SheriffOrder sheriffOrder) {
        sheriffOrder.setDelete(Boolean.TRUE.equals(details.getDelete()));
        sheriffOrder.setRemoteFeedName(details.getFeedName());
        sheriffOrder.setCategory(details.getCategory());
        sheriffOrder.setReasonCode(details.getReasonCode());
        sheriffOrder.setReasonDetails(details.getReasonDetails());
        sheriffOrder.setCreatedAt(Util.toTimestamp(details.getCreatedAt()));
        sheriffOrder.setSignature(details.getSignature());
        sheriffOrder.setSignatureVersion(details.getSignatureVersion());
    }

}
