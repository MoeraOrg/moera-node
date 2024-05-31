package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheriffComplaintInfo {

    private String id;
    private String ownerName;
    private String ownerFullName;
    private String ownerGender;
    private SheriffComplaintGroupInfo group;
    private SheriffOrderReason reasonCode;
    private String reasonDetails;
    private boolean anonymousRequested;
    private long createdAt;

    public SheriffComplaintInfo() {
    }

    public SheriffComplaintInfo(SheriffComplaint sheriffComplaint, boolean withGroup) {
        id = sheriffComplaint.getId().toString();
        ownerName = sheriffComplaint.getOwnerName();
        ownerFullName = sheriffComplaint.getOwnerFullName();
        ownerGender = sheriffComplaint.getOwnerGender();
        if (withGroup && sheriffComplaint.getGroup() != null) {
            group = new SheriffComplaintGroupInfo(sheriffComplaint.getGroup());
        }
        reasonCode = sheriffComplaint.getReasonCode();
        reasonDetails = sheriffComplaint.getReasonDetails();
        anonymousRequested = sheriffComplaint.isAnonymousRequested();
        createdAt = Util.toEpochSecond(sheriffComplaint.getCreatedAt());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

    public SheriffComplaintGroupInfo getGroup() {
        return group;
    }

    public void setGroup(SheriffComplaintGroupInfo group) {
        this.group = group;
    }

    public SheriffOrderReason getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SheriffOrderReason reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDetails() {
        return reasonDetails;
    }

    public void setReasonDetails(String reasonDetails) {
        this.reasonDetails = reasonDetails;
    }

    public boolean isAnonymousRequested() {
        return anonymousRequested;
    }

    public void setAnonymousRequested(boolean anonymousRequested) {
        this.anonymousRequested = anonymousRequested;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
