package org.moera.node.model;

import javax.validation.constraints.Size;

import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintStatus;

public class SheriffComplaintDecisionText {

    private boolean reject;

    private SheriffOrderReason decisionCode;

    @Size(max = 4096)
    private String decisionDetails;

    private Boolean anonymous;

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

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public void toSheriffComplaintGroup(SheriffComplaintGroup sheriffComplaintGroup) {
        if (reject) {
            sheriffComplaintGroup.setStatus(SheriffComplaintStatus.REJECTED);
            sheriffComplaintGroup.setDecisionCode(null);
        } else {
            sheriffComplaintGroup.setStatus(SheriffComplaintStatus.APPROVED);
            sheriffComplaintGroup.setDecisionCode(decisionCode);
        }
        sheriffComplaintGroup.setDecisionDetails(decisionDetails);
        if (anonymous != null) {
            sheriffComplaintGroup.setAnonymous(anonymous);
        }
    }

}
