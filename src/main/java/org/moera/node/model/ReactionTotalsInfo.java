package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.data.ReactionTotal;

public class ReactionTotalsInfo {

    private List<ReactionTotalInfo> positive = new ArrayList<>();
    private List<ReactionTotalInfo> negative = new ArrayList<>();

    public ReactionTotalsInfo() {
    }

    public ReactionTotalsInfo(Collection<ReactionTotal> totals, boolean countsVisible) {
        int sum = 0;
        if (!countsVisible) {
            sum = (int) totals.stream()
                    .collect(Collectors.summarizingInt(ReactionTotal::getTotal))
                    .getSum();
        }
        for (ReactionTotal total : totals) {
            if (total.getTotal() == 0) {
                continue;
            }
            ReactionTotalInfo info = countsVisible
                    ? ReactionTotalInfo.countsInfo(total) : ReactionTotalInfo.shareInfo(total, sum);
            if (!total.isNegative()) {
                positive.add(info);
            } else {
                negative.add(info);
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
