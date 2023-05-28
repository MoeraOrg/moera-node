package org.moera.node.model;

import javax.validation.constraints.Size;

import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainStatus;

public class SheriffComplainDecisionText {

    private boolean reject;

    private SheriffOrderReason decisionCode;

    @Size(max = 4096)
    private String decisionDetails;

    public boolean isReject() {
        return reject;
    }

    public void setReject(boolean reject) {
        this.reject = reject;
    }

    public SheriffOrderReason getDecisionCode() {
        return decisionCode;
    }

    public void setDecisionCode(SheriffOrderReason decisionCode) {
        this.decisionCode = decisionCode;
    }

    public String getDecisionDetails() {
        return decisionDetails;
    }

    public void setDecisionDetails(String decisionDetails) {
        this.decisionDetails = decisionDetails;
    }

    public void toSheriffComplainGroup(SheriffComplainGroup sheriffComplainGroup) {
        if (reject) {
            sheriffComplainGroup.setStatus(SheriffComplainStatus.REJECTED);
            sheriffComplainGroup.setDecisionCode(null);
        } else {
            sheriffComplainGroup.setStatus(SheriffComplainStatus.ACCEPTED);
            sheriffComplainGroup.setDecisionCode(decisionCode);
        }
        sheriffComplainGroup.setDecisionDetails(decisionDetails);
    }

}
