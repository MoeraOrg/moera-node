package org.moera.node.model;

import org.moera.lib.node.types.StorySummarySheriff;

public class StorySummarySheriffUtil {

    public static StorySummarySheriff build(String sheriffName, String orderId, String complaintId) {
        StorySummarySheriff sheriff = new StorySummarySheriff();
        sheriff.setSheriffName(sheriffName);
        sheriff.setOrderId(orderId);
        sheriff.setComplaintId(complaintId);
        return sheriff;
    }

}
