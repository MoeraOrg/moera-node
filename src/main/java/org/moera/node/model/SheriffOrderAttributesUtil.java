package org.moera.node.model;

import org.moera.lib.node.types.SheriffComplaintDecisionText;
import org.moera.lib.node.types.SheriffOrderAttributes;
import org.moera.lib.node.types.SheriffOrderCategory;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.node.data.SheriffComplaintGroup;

public class SheriffOrderAttributesUtil {

    public static SheriffOrderAttributes build(
        SheriffComplaintGroup group, SheriffOrderCategory category, SheriffComplaintDecisionText decisionText
    ) {
        SheriffOrderAttributes attributes = new SheriffOrderAttributes();
        attributes.setDelete(decisionText.isReject());
        attributes.setFeedName(group.getRemoteFeedName());
        attributes.setPostingId(group.getRemotePostingId());
        attributes.setCommentId(group.getRemoteCommentId());
        attributes.setCategory(category);
        
        SheriffOrderReason reasonCode = decisionText.getDecisionCode();
        if (reasonCode == null) {
            reasonCode = SheriffOrderReason.OTHER;
        }
        attributes.setReasonCode(reasonCode);
        attributes.setReasonDetails(decisionText.getDecisionDetails());
        
        return attributes;
    }

}
