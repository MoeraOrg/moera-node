package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeopleGeneralInfo {

    private Integer feedSubscribersTotal;
    private Integer feedSubscriptionsTotal;

    public PeopleGeneralInfo() {
    }

    public PeopleGeneralInfo(Integer feedSubscribersTotal, Integer feedSubscriptionsTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
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

}
