package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedStatus {

    private int total;
    private int totalPinned;
    private Integer notViewed;
    private Integer notRead;
    private Long notViewedMoment;

    public FeedStatus() {
    }

    public FeedStatus(int total, int totalPinned, int notViewed, int notRead, Long notViewedMoment) {
        this.total = total;
        this.totalPinned = totalPinned;
        this.notViewed = notViewed;
        this.notRead = notRead;
        this.notViewedMoment = notViewedMoment;
    }

    public FeedStatus(int total, int totalPinned) {
        this.total = total;
        this.totalPinned = totalPinned;
    }

    public FeedStatus notAdmin() {
        return new FeedStatus(total, totalPinned);
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

}
