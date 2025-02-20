package org.moera.node.model;

import org.moera.lib.node.types.SheriffComplaintDecisionText;
import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.node.data.SheriffComplaintGroup;

public class SheriffComplaintDecisionTextUtil {

    public static void toSheriffComplaintGroup(
        SheriffComplaintDecisionText decisionText, SheriffComplaintGroup sheriffComplaintGroup
    ) {
        if (decisionText.isReject()) {
            sheriffComplaintGroup.setStatus(SheriffComplaintStatus.REJECTED);
            sheriffComplaintGroup.setDecisionCode(null);
        } else {
            sheriffComplaintGroup.setStatus(SheriffComplaintStatus.APPROVED);
            sheriffComplaintGroup.setDecisionCode(decisionText.getDecisionCode());
        }
        sheriffComplaintGroup.setDecisionDetails(decisionText.getDecisionDetails());
        if (decisionText.getAnonymous() != null) {
            sheriffComplaintGroup.setAnonymous(decisionText.getAnonymous());
        }
    }

}
