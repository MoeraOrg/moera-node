package org.moera.node.ui;

public class PaginationItem {

    private String title;
    private Long moment;
    private boolean active;
    private boolean dots;

    public PaginationItem() {
    }

    private PaginationItem(String title, Long moment, boolean active, boolean dots) {
        this.title = title;
        this.moment = moment;
        this.active = active;
        this.dots = dots;
    }

    public static PaginationItem pageLink(String title, long moment, boolean active) {
        return new PaginationItem(title, moment, active, false);
    }

    public static PaginationItem pageLink(int n, long moment, boolean active) {
        return pageLink(Integer.toString(n), moment, active);
    }

    public static PaginationItem pageDots() {
        return new PaginationItem(null, null, false, true);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public boolean isFirst() {
        return moment == Long.MAX_VALUE;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDots() {
        return dots;
    }

    public void setDots(boolean dots) {
        this.dots = dots;
    }

}
