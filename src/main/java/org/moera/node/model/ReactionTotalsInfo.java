package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.moera.node.data.ReactionTotal;

public class ReactionTotalsInfo {

    private List<ReactionTotalInfo> positive = new ArrayList<>();
    private List<ReactionTotalInfo> negative = new ArrayList<>();

    public ReactionTotalsInfo() {
    }

    public ReactionTotalsInfo(Collection<ReactionTotal> totals) {
        for (ReactionTotal total : totals) {
            if (!total.isNegative()) {
                positive.add(new ReactionTotalInfo(total));
            } else {
                negative.add(new ReactionTotalInfo(total));
            }
        }
    }

    public List<ReactionTotalInfo> getPositive() {
        return positive;
    }

    public void setPositive(List<ReactionTotalInfo> positive) {
        this.positive = positive;
    }

    public List<ReactionTotalInfo> getNegative() {
        return negative;
    }

    public void setNegative(List<ReactionTotalInfo> negative) {
        this.negative = negative;
    }

}
