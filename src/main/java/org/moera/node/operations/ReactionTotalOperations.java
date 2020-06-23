package org.moera.node.operations;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Posting;
import org.moera.node.data.ReactionTotal;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ReactionTotalInfo;
import org.moera.node.model.ReactionTotalsInfo;
import org.springframework.stereotype.Component;

@Component
public class ReactionTotalOperations {

    private static class ReactionIndex {

        public boolean negative;
        public int emoji;

        ReactionIndex(ReactionTotal reactionTotal) {
            negative = reactionTotal.isNegative();
            emoji = reactionTotal.getEmoji();
        }

        ReactionIndex(boolean negative, int emoji) {
            this.negative = negative;
            this.emoji = emoji;
        }

        @Override
        public boolean equals(Object peer) {
            if (this == peer) {
                return true;
            }
            if (peer == null || getClass() != peer.getClass()) {
                return false;
            }
            ReactionIndex that = (ReactionIndex) peer;
            return negative == that.negative && emoji == that.emoji;
        }

        @Override
        public int hashCode() {
            return Objects.hash(negative, emoji);
        }

    }

    public static class ReactionTotalsData {

        private final RequestContext requestContext;
        private final Posting posting;
        private final Set<ReactionTotal> totals;

        ReactionTotalsData(RequestContext requestContext, Posting posting, Set<ReactionTotal> totals) {
            this.requestContext = requestContext;
            this.posting = posting;
            this.totals = totals;
        }

        public boolean isVisibleToClient() {
            return isVisibleToPublic()
                    || (requestContext.isAdmin() || requestContext.isClient(posting.getOwnerName()))
                        && posting.isOriginal();
        }

        public boolean isVisibleToPublic() {
            return posting.isReactionTotalsVisible();
        }

        public ReactionTotalsInfo getClientInfo() {
            return new ReactionTotalsInfo(totals, isVisibleToClient());
        }

        public ReactionTotalsInfo getPublicInfo() {
            return new ReactionTotalsInfo(totals, isVisibleToPublic());
        }

    }

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    public void replaceAll(Posting posting, ReactionTotalsInfo reactionTotalsInfo) {
        reactionTotalRepository.deleteAllByEntryId(posting.getId());
        replaceAll(posting, false, reactionTotalsInfo.getPositive());
        replaceAll(posting, true, reactionTotalsInfo.getNegative());
    }

    private void replaceAll(Posting posting, boolean negative, List<ReactionTotalInfo> reactionTotalList) {
        for (ReactionTotalInfo reactionTotalInfo : reactionTotalList) {
            ReactionTotal reactionTotal = new ReactionTotal();
            reactionTotal.setId(UUID.randomUUID());
            reactionTotal.setEntry(posting);
            reactionTotal.setNegative(negative);
            reactionTotal.setEmoji(reactionTotalInfo.getEmoji());
            reactionTotal.setTotal(realOrVirtualTotal(reactionTotalInfo));
            reactionTotalRepository.save(reactionTotal);
        }
    }

    public boolean isSame(Set<ReactionTotal> reactionTotals, ReactionTotalsInfo reactionTotalsInfo) {
        if (reactionTotals.size() != reactionTotalsInfo.getPositive().size() + reactionTotalsInfo.getNegative().size()) {
            return false;
        }
        Map<ReactionIndex, Integer> totals = reactionTotals.stream()
                .collect(Collectors.toMap(ReactionIndex::new, ReactionTotal::getTotal, (ri1, ri2) -> ri2));
        return containsAll(reactionTotalsInfo.getPositive(), false, totals)
                && containsAll(reactionTotalsInfo.getNegative(), true, totals);
    }

    private boolean containsAll(List<ReactionTotalInfo> reactionTotalList, boolean negative,
                                Map<ReactionIndex, Integer> totals) {
        for (ReactionTotalInfo info : reactionTotalList) {
            int total = totals.getOrDefault(new ReactionIndex(negative, info.getEmoji()), 0);
            if (total != realOrVirtualTotal(info)) {
                return false;
            }
        }
        return true;
    }

    public boolean isVisibleToClient(Posting posting) {
        return posting.isReactionTotalsVisible() || requestContext.isAdmin()
                || requestContext.isClient(posting.getOwnerName());
    }

    public ReactionTotalsData getInfo(Posting posting) {
        Set<ReactionTotal> totals = reactionTotalRepository.findAllByEntryId(posting.getId());
        return new ReactionTotalsData(requestContext, posting, totals);
    }

    private int realOrVirtualTotal(ReactionTotalInfo info) {
        return info.getTotal() != null ? info.getTotal() : (int) (info.getShare() * 1000);
    }

}
