package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
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
        private final Entry entry;
        private final Collection<ReactionTotal> totals;

        ReactionTotalsData(RequestContext requestContext, Entry entry, Collection<ReactionTotal> totals) {
            this.requestContext = requestContext;
            this.entry = entry;
            this.totals = totals;
        }

        public ReactionTotalsInfo getClientInfo() {
            return new ReactionTotalsInfo(totals, entry, requestContext);
        }

        public ReactionTotalsInfo getPublicInfo() {
            return new ReactionTotalsInfo(totals, entry, AccessCheckers.PUBLIC);
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
            reactionTotal.setForged(reactionTotalInfo.getTotal() == null);
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

    public ReactionTotalsData getInfo(Entry entry) {
        Set<ReactionTotal> totals = reactionTotalRepository.findAllByEntryId(entry.getId());
        return new ReactionTotalsData(requestContext, entry, totals);
    }

    public List<ReactionTotalsData> getInfo(List<? extends Entry> entries) {
        List<UUID> ids = entries.stream().map(Entry::getId).collect(Collectors.toList());
        Map<UUID, List<ReactionTotal>> totals = new HashMap<>();
        reactionTotalRepository.findAllByEntryIds(ids).forEach(rt -> {
            List<ReactionTotal> list = totals.computeIfAbsent(rt.getEntry().getId(), id -> new ArrayList<>());
            list.add(rt);
        });
        return entries.stream()
                .filter(e -> totals.containsKey(e.getId()))
                .map(e -> new ReactionTotalsData(requestContext, e, totals.get(e.getId())))
                .collect(Collectors.toList());
    }

    private int realOrVirtualTotal(ReactionTotalInfo info) {
        return info.getTotal() != null ? info.getTotal() : (int) (info.getShare() * 1000);
    }

    public void changeTotals(Entry entry, Reaction reaction, int delta) {
        changeEntryRevisionTotal(reaction.getEntryRevision(), reaction.isNegative(), reaction.getEmoji(), delta);
        changeEntryTotal(entry, reaction.isNegative(), reaction.getEmoji(), delta);
    }

    private void changeEntryRevisionTotal(EntryRevision entryRevision, boolean negative, int emoji, int delta) {
        ReactionTotal total = reactionTotalRepository.findByEntryRevisionId(entryRevision.getId(), negative, emoji);
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntryRevision(entryRevision);
            total.setNegative(negative);
            total.setEmoji(emoji);
            total.setTotal(delta);
            total = reactionTotalRepository.save(total);
            entryRevision.addReactionTotal(total);
        } else {
            total.setTotal(total.getTotal() + delta);
        }
    }

    public void changeEntryTotal(Entry entry, boolean negative, int emoji, int delta) {
        ReactionTotal total = reactionTotalRepository.findByEntryId(entry.getId(), negative, emoji);
        if (total == null) {
            total = new ReactionTotal();
            total.setId(UUID.randomUUID());
            total.setEntry(entry);
            total.setNegative(negative);
            total.setEmoji(emoji);
            total.setTotal(delta);
            total = reactionTotalRepository.save(total);
            entry.addReactionTotal(total);
        } else {
            total.setTotal(total.getTotal() + delta);
        }
    }

}
