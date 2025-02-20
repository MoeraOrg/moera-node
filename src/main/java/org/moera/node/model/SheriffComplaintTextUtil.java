package org.moera.node.model;

import org.moera.lib.node.types.SheriffComplaintText;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;

public class SheriffComplaintTextUtil {

    public static void toSheriffComplaint(SheriffComplaintText complaintText, SheriffComplaint sheriffComplaint) {
        sheriffComplaint.setOwnerFullName(complaintText.getOwnerFullName());
        sheriffComplaint.setOwnerGender(complaintText.getOwnerGender());
        sheriffComplaint.setReasonCode(
            complaintText.getReasonCode() != null ? complaintText.getReasonCode() : SheriffOrderReason.OTHER
        );
        sheriffComplaint.setReasonDetails(complaintText.getReasonDetails());
        if (complaintText.getAnonymous() != null) {
            sheriffComplaint.setAnonymousRequested(complaintText.getAnonymous());
        }
    }

    public static void toSheriffComplaintGroup(
        SheriffComplaintText complaintText, SheriffComplaintGroup sheriffComplaintGroup
    ) {
        sheriffComplaintGroup.setRemoteNodeName(complaintText.getNodeName());
        sheriffComplaintGroup.setRemoteNodeFullName(complaintText.getFullName());
        sheriffComplaintGroup.setRemoteFeedName(complaintText.getFeedName());
        sheriffComplaintGroup.setRemotePostingId(complaintText.getPostingId());
        sheriffComplaintGroup.setRemotePostingOwnerName(complaintText.getPostingOwnerName());
        sheriffComplaintGroup.setRemotePostingOwnerFullName(complaintText.getPostingOwnerFullName());
        sheriffComplaintGroup.setRemotePostingOwnerGender(complaintText.getPostingOwnerGender());
        sheriffComplaintGroup.setRemotePostingHeading(complaintText.getPostingHeading());
        sheriffComplaintGroup.setRemoteCommentId(complaintText.getCommentId());
        sheriffComplaintGroup.setRemoteCommentOwnerName(complaintText.getCommentOwnerName());
        sheriffComplaintGroup.setRemoteCommentOwnerFullName(complaintText.getCommentOwnerFullName());
        sheriffComplaintGroup.setRemoteCommentOwnerGender(complaintText.getOwnerGender());
        sheriffComplaintGroup.setRemoteCommentHeading(complaintText.getCommentHeading());
    }

}
