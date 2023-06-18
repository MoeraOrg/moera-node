package org.moera.node.model;

import java.util.List;

public class UserListSliceInfo {

    private String listName;
    private long before;
    private long after;
    private List<UserListItemInfo> items;
    private int total;
    private int totalInPast;
    private int totalInFuture;

    public UserListSliceInfo() {
    }

    public UserListSliceInfo(String listName) {
        this.listName = listName;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public long getBefore() {
        return before;
    }

    public void setBefore(long before) {
        this.before = before;
    }

    public long getAfter() {
        return after;
    }

    public void setAfter(long after) {
        this.after = after;
    }

    public List<UserListItemInfo> getItems() {
        return items;
    }

    public void setItems(List<UserListItemInfo> items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalInPast() {
        return totalInPast;
    }

    public void setTotalInPast(int totalInPast) {
        this.totalInPast = totalInPast;
    }

    public int getTotalInFuture() {
        return totalInFuture;
    }

    public void setTotalInFuture(int totalInFuture) {
        this.totalInFuture = totalInFuture;
    }

}
