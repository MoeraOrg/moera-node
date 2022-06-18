package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeopleGeneralInfo {

    private Integer feedSubscribersTotal;
    private Integer feedSubscriptionsTotal;
    private Map<String, Principal> operations;

    public PeopleGeneralInfo() {
    }

    public PeopleGeneralInfo(Integer feedSubscribersTotal, Integer feedSubscriptionsTotal, Options options) {
        this.feedSubscribersTotal = feedSubscribersTotal;
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
        operations = new HashMap<>();
        putOperation(operations, "viewSubscribers", options.getPrincipal("subscribers.view"), Principal.PUBLIC);
        putOperation(operations, "viewSubscriptions", options.getPrincipal("subscriptions.view"), Principal.PUBLIC);
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public Integer getFeedSubscribersTotal() {
        return feedSubscribersTotal;
    }

    public void setFeedSubscribersTotal(Integer feedSubscribersTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    public Integer getFeedSubscriptionsTotal() {
        return feedSubscriptionsTotal;
    }

    public void setFeedSubscriptionsTotal(Integer feedSubscriptionsTotal) {
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
