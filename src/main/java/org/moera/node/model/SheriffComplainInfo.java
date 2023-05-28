package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SheriffComplain;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheriffComplainInfo {

    private String id;
    private String ownerName;
    private String ownerFullName;
    private String ownerGender;
    private SheriffComplainGroupInfo group;
    private SheriffOrderReason reasonCode;
    private String reasonDetails;
    private long createdAt;

    public SheriffComplainInfo() {
    }

    public SheriffComplainInfo(SheriffComplain sheriffComplain, boolean withGroup) {
        id = sheriffComplain.getId().toString();
        ownerName = sheriffComplain.getOwnerName();
        ownerFullName = sheriffComplain.getOwnerFullName();
        ownerGender = sheriffComplain.getOwnerGender();
        if (withGroup && sheriffComplain.getGroup() != null) {
            group = new SheriffComplainGroupInfo(sheriffComplain.getGroup());
        }
        reasonCode = sheriffComplain.getReasonCode();
        reasonDetails = sheriffComplain.getReasonDetails();
        createdAt = Util.toEpochSecond(sheriffComplain.getCreatedAt());
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

    public SheriffComplainGroupInfo getGroup() {
        return group;
    }

    public void setGroup(SheriffComplainGroupInfo group) {
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
