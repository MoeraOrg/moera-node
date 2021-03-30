package org.moera.node.model.event;

public class PeopleChangedEvent extends Event {

    private int feedSubscribersTotal;
    private int feedSubscriptionsTotal;

    public PeopleChangedEvent() {
        super(EventType.PEOPLE_CHANGED);
    }

    public PeopleChangedEvent(int feedSubscribersTotal, int feedSubscriptionsTotal) {
        this();
        this.feedSubscribersTotal = feedSubscribersTotal;
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

    public int getFeedSubscribersTotal() {
        return feedSubscribersTotal;
    }

    public void setFeedSubscribersTotal(int feedSubscribersTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    public int getFeedSubscriptionsTotal() {
        return feedSubscriptionsTotal;
    }

    public void setFeedSubscriptionsTotal(int feedSubscriptionsTotal) {
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

}
