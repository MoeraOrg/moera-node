package org.moera.node.ui;

public record PaginationItem(
    String title,
    Long moment,
    boolean active,
    boolean dots
) {

    public PaginationItem() {
        this(null, null, false, false);
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

    public boolean isFirst() {
        return moment == Long.MAX_VALUE;
    }

}
