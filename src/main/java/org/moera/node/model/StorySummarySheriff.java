package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummarySheriff {

    private String sheriffName;
    private String orderId;

    public StorySummarySheriff() {
    }

    public StorySummarySheriff(String sheriffName, String orderId) {
        this.sheriffName = sheriffName;
        this.orderId = orderId;
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

}
