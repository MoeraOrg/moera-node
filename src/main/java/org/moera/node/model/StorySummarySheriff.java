package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummarySheriff {

    private String sheriffName;
    private String orderId;
    private String complaintId;

    public StorySummarySheriff() {
    }

    public StorySummarySheriff(String sheriffName, String orderId, String complaintId) {
        this.sheriffName = sheriffName;
        this.orderId = orderId;
        this.complaintId = complaintId;
    }

    public String getSheriffName() {
        return sheriffName;
    }

    public void setSheriffName(String sheriffName) {
        this.sheriffName = sheriffName;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

}
