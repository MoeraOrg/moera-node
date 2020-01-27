package org.moera.node.model;

import java.util.Collections;
import java.util.List;

public class ReactionsSliceInfo {

    public static final ReactionsSliceInfo EMPTY = ReactionsSliceInfo.empty();

    private long before;
    private long after;
    private int total;
    private List<ReactionInfo> reactions;

    public ReactionsSliceInfo() {
    }

    private static ReactionsSliceInfo empty() {
        ReactionsSliceInfo info = new ReactionsSliceInfo();
        info.before = Long.MAX_VALUE;
        info.after = Long.MIN_VALUE;
        info.total = 0;
        info.reactions = Collections.emptyList();
        return info;
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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<ReactionInfo> getReactions() {
        return reactions;
    }

    public void setReactions(List<ReactionInfo> reactions) {
        this.reactions = reactions;
    }

}
