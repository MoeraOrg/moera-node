package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedStatus {

    private int total;
    private int totalPinned;
    private Long lastMoment;
    private Integer notViewed;
    private Integer notRead;
    private Long notViewedMoment;
    private Long notReadMoment;

    public FeedStatus() {
    }

    public FeedStatus(int total, int totalPinned, Long lastMoment, Integer notViewed, Integer notRead,
                      Long notViewedMoment, Long notReadMoment) {
        this.total = total;
        this.totalPinned = totalPinned;
        this.lastMoment = lastMoment;
        this.notViewed = notViewed;
        this.notRead = notRead;
        this.notViewedMoment = notViewedMoment;
        this.notReadMoment = notReadMoment;
    }

    public FeedStatus(int total, int totalPinned, Long lastMoment) {
        this.total = total;
        this.totalPinned = totalPinned;
        this.lastMoment = lastMoment;
    }

    public FeedStatus notAdmin() {
        return new FeedStatus(total, totalPinned, lastMoment);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPinned() {
        return totalPinned;
    }

    public void setTotalPinned(int totalPinned) {
        this.totalPinned = totalPinned;
    }

    public Long getLastMoment() {
        return lastMoment;
    }

    public void setLastMoment(Long lastMoment) {
        this.lastMoment = lastMoment;
    }

    public Integer getNotViewed() {
        return notViewed;
    }

    public void setNotViewed(Integer notViewed) {
        this.notViewed = notViewed;
    }

    public Integer getNotRead() {
        return notRead;
    }

    public void setNotRead(Integer notRead) {
        this.notRead = notRead;
    }

    public Long getNotViewedMoment() {
        return notViewedMoment;
    }

    public void setNotViewedMoment(Long notViewedMoment) {
        this.notViewedMoment = notViewedMoment;
    }

    public Long getNotReadMoment() {
        return notReadMoment;
    }

    public void setNotReadMoment(Long notReadMoment) {
        this.notReadMoment = notReadMoment;
    }

}
