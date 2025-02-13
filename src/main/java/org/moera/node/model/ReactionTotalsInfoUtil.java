package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.moera.lib.node.types.ReactionTotalInfo;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.Entry;
import org.moera.node.data.ReactionTotal;

public class ReactionTotalsInfoUtil {

    public static ReactionTotalsInfo build(Collection<ReactionTotal> totals, Entry entry, AccessChecker accessChecker) {
        ReactionTotalsInfo totalsInfo = new ReactionTotalsInfo();
        totalsInfo.setEntryId(entry.getId().toString());
        totalsInfo.setPositive(new ArrayList<>());
        totalsInfo.setNegative(new ArrayList<>());

        boolean forged = totals.stream().anyMatch(ReactionTotal::isForged);
        boolean totalsVisible = !forged && isTotalsVisible(entry, accessChecker);
        boolean negativeTotalsVisible = !forged && isNegativeTotalsVisible(entry, accessChecker);
        boolean ratiosVisible = isRatiosVisible(entry, accessChecker);
        boolean negativeRatiosVisible = isNegativeRatiosVisible(entry, accessChecker);

        if (!totalsVisible && !ratiosVisible) {
            return totalsInfo;
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
                ? ReactionTotalInfoUtil.countsInfo(total)
                : ReactionTotalInfoUtil.shareInfo(total, sum);
            if (!total.isNegative()) {
                totalsInfo.getPositive().add(info);
            } else {
                if (totalsVisible && negativeTotalsVisible || !totalsVisible && negativeRatiosVisible) {
                    totalsInfo.getNegative().add(info);
                }
            }
        }

        return totalsInfo;
    }

    private static boolean isTotalsVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionsE().a()
                .or(entry.getViewReactionTotalsE()), Scope.VIEW_CONTENT);
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionsE().a()
                .or(entry.getReceiverViewReactionTotalsE()), Scope.VIEW_CONTENT);
        }
    }

    private static boolean isNegativeTotalsVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionsE().a()
                .or(entry.getViewNegativeReactionsE()), Scope.VIEW_CONTENT)
                || accessChecker.isPrincipal(entry.getViewNegativeReactionTotalsE(), Scope.VIEW_CONTENT);
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionsE().a()
                .or(entry.getReceiverViewNegativeReactionsE()), Scope.VIEW_CONTENT)
                || accessChecker.isPrincipal(entry.getReceiverViewNegativeReactionTotalsE(), Scope.VIEW_CONTENT);
        }
    }

    private static boolean isRatiosVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewReactionRatiosE(), Scope.VIEW_CONTENT);
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewReactionRatiosE(), Scope.VIEW_CONTENT);
        }
    }

    private static boolean isNegativeRatiosVisible(Entry entry, AccessChecker accessChecker) {
        if (entry.isOriginal()) {
            return accessChecker.isPrincipal(entry.getViewNegativeReactionRatiosE(), Scope.VIEW_CONTENT);
        } else {
            return accessChecker.isPrincipal(entry.getReceiverViewNegativeReactionRatiosE(), Scope.VIEW_CONTENT);
        }
    }

}
