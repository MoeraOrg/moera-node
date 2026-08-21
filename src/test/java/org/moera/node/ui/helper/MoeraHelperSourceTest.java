package org.moera.node.ui.helper;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.ReactionTotalInfo;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.Principal;

class MoeraHelperSourceTest {

    private final MoeraHelperSource helperSource = new MoeraHelperSource();

    @Test
    void showsMostPopularEmojisWithoutTotals() {
        PostingInfo posting = posting(
            List.of(
                reaction(0x1f600, .2f),
                reaction(0x1f601, .4f),
                reaction(0x1f602, .1f),
                reaction(0x1f603, .3f)
            ),
            Principal.PRIVATE,
            Principal.PRIVATE
        );

        Assertions.assertEquals(
            "<div class=\"reactions\"><span class=\"positive\"><span class=\"emojis\">"
                + "&#128513;&#128515;&#128512;</span></span></div>",
            helperSource.reactions(posting).toString()
        );
    }

    @Test
    void showsTotalsWhenPermitted() {
        PostingInfo posting = posting(
            List.of(reaction(0x1f600, 2), reaction(0x1f601, 5)),
            Principal.PRIVATE,
            Principal.PUBLIC
        );

        Assertions.assertEquals(
            "<div class=\"reactions\"><span class=\"positive\"><span class=\"emojis\">"
                + "&#128513;&#128512;</span>7</span></div>",
            helperSource.reactions(posting).toString()
        );
    }

    @Test
    void showsNothingWhenReactionsAreNotAvailable() {
        PostingInfo posting = posting(Collections.emptyList(), Principal.PRIVATE, Principal.PRIVATE);

        Assertions.assertEquals(
            "<div class=\"reactions\"></div>",
            helperSource.reactions(posting).toString()
        );
    }

    private static PostingInfo posting(
        List<ReactionTotalInfo> positive, Principal viewReactions, Principal viewReactionTotals
    ) {
        ReactionTotalsInfo reactions = new ReactionTotalsInfo();
        reactions.setPositive(positive);
        reactions.setNegative(Collections.emptyList());

        PostingOperations operations = new PostingOperations();
        operations.setViewReactions(viewReactions);
        operations.setViewReactionTotals(viewReactionTotals);

        PostingInfo posting = new PostingInfo();
        posting.setOperations(operations);
        posting.setReactions(reactions);
        return posting;
    }

    private static ReactionTotalInfo reaction(int emoji, int total) {
        ReactionTotalInfo reaction = new ReactionTotalInfo();
        reaction.setEmoji(emoji);
        reaction.setTotal(total);
        return reaction;
    }

    private static ReactionTotalInfo reaction(int emoji, float share) {
        ReactionTotalInfo reaction = new ReactionTotalInfo();
        reaction.setEmoji(emoji);
        reaction.setShare(share);
        return reaction;
    }

}
