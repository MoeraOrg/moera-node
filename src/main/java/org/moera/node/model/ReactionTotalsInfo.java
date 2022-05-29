package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Entry;
import org.moera.node.data.ReactionTotal;

public class ReactionTotalsInfo {

    private List<ReactionTotalInfo> positive = new ArrayList<>();
    private List<ReactionTotalInfo> negative = new ArrayList<>();

    public ReactionTotalsInfo() {
    }

    public ReactionTotalsInfo(Collection<ReactionTotal> totals, Entry entry, AccessChecker accessChecker) {
        boolean forged = totals.stream().anyMatch(ReactionTotal::isForged);
        boolean totalsVisible = !forged && isTotalsVisible(entry, accessChecker);
        boolean negativeTotalsVisible = !forged && isNegativeTotalsVisible(entry, accessChecker);
        boolean ratiosVisible = isRatiosVisible(entry, accessChecker);
        boolean negativeRatiosVisible = isNegativeRatiosVisible(entry, accessChecker);

        if (!totalsVisible && !ratiosVisible) {
            return;
        }
        int sum = 0;
        if (!totalsVisible) {
            sum = (int) totals.stream()
                    .collect(Collectors.summarizingInt(ReactionTotal::getTotal))
                    .getSum();
        }
        for (ReactionTotal total : totals) {
            if (total.getTotal() == 0) {
                continue;
            }
            ReactionTotalInfo info = totalsVisible
                    ? ReactionTotalInfo.countsInfo(total)
                    : ReactionTotalInfo.shareInfo(total, sum);
            if (!total.isNegative()) {
                positive.add(info);
            } else {
                if (totalsVisible && negativeTotalsVisible || !totalsVisible && negativeRatiosVisible) {
                    negative.add(info);
                }
            }
        }
    }

    private static boolean isTotalsVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionsE().a()
                    .or(entry.getViewReactionTotalsE()));
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionsE().a()
                    .or(entry.getReceiverViewReactionTotalsE()));
        }
    }

    private static boolean isNegativeTotalsVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionsE().a()
                    .or(entry.getViewNegativeReactionsE()))
                    || accessChecker.isPrincipal(entry.getViewNegativeReactionTotalsE());
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionsE().a()
                    .or(entry.getReceiverViewNegativeReactionsE()))
                    || accessChecker.isPrincipal(entry.getReceiverViewNegativeReactionTotalsE());
        }
    }

    private static boolean isRatiosVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionRatiosE());
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionRatiosE());
        }
    }

    private static boolean isNegativeRatiosVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewNegativeReactionRatiosE());
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewNegativeReactionRatiosE());
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
