package org.moera.node.rest.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.RecommendedPostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.Feed;
import org.moera.node.data.Pick;
import org.moera.node.global.UniversalContext;
import org.moera.node.option.Options;
import org.moera.node.picker.PickerPool;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FetchRecommendationJobTest {

    private final MoeraNode search = mock(MoeraNode.class);
    private final PickerPool pickerPool = mock(PickerPool.class);

    private final FetchRecommendationJob job = new FetchRecommendationJob() {
        @Override
        protected String generateCarte(String targetNodeName, Scope clientScope) {
            return "carte";
        }
    };

    @BeforeEach
    void setUp() throws MoeraNodeException {
        Options options = mock(Options.class);
        when(options.getString("recommendations.source")).thenReturn("search");
        UniversalContext context = mock(UniversalContext.class);
        when(context.getOptions()).thenReturn(options);
        NodeApi nodeApi = mock(NodeApi.class);
        when(nodeApi.at("search", "carte")).thenReturn(search);
        ReflectionTestUtils.setField(job, "universalContext", context);
        ReflectionTestUtils.setField(job, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(job, "pickerPool", pickerPool);
    }

    @ParameterizedTest
    @CsvSource({"1, 6", "5, 10", "20, 40"})
    void requestsTwiceTheLimitAndAtLeastFiveExtra(int limit, int requestLimit) throws MoeraNodeException {
        execute(limit);

        verify(search).getRecommendedPostings(Feed.NEWS, null, requestLimit);
    }

    @Test
    void picksAndAcceptsOnlySelectedRecommendations() throws MoeraNodeException {
        recommendations(posting("negative", -10), posting("default", null), posting("boosted", Integer.MAX_VALUE));

        execute(2);

        List<String> picked = pickedPostingIds(2);
        assertEquals(2, Set.copyOf(picked).size());
        assertTrue(Set.of("negative", "default", "boosted").containsAll(picked));
        ArgumentCaptor<String> accepted = ArgumentCaptor.forClass(String.class);
        verify(search, times(2)).acceptRecommendedPosting(eq("author"), accepted.capture(), eq(Feed.NEWS));
        assertEquals(picked, accepted.getAllValues());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void picksAllRecommendationsWhenTheyFitTheLimit(int limit) throws MoeraNodeException {
        recommendations(posting("negative", -10), posting("default", null));

        execute(limit);

        assertEquals(List.of("negative", "default"), pickedPostingIds(2));
    }

    @Test
    void selectsInProportionToBenefitPlusOneWithMinimumWeightOfOne() {
        var negativeBenefit = posting("negative", -10);
        var defaultBenefit = posting("default", null);
        var zeroBenefit = posting("zero", 0);
        var boosted = posting("boosted", 2);
        var candidates = new RecommendedPostingInfo[] {negativeBenefit, defaultBenefit, zeroBenefit, boosted};
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextLong(6L)).thenReturn(0L, 1L, 2L, 3L, 4L, 5L);

        var selected = new ArrayList<RecommendedPostingInfo>();
        for (int i = 0; i < 6; i++) {
            selected.add(FetchRecommendationJob.selectRecommendations(candidates, 1, random).getFirst());
        }

        assertEquals(1, Collections.frequency(selected, negativeBenefit));
        assertEquals(1, Collections.frequency(selected, defaultBenefit));
        assertEquals(1, Collections.frequency(selected, zeroBenefit));
        assertEquals(3, Collections.frequency(selected, boosted));
    }

    private void execute(int limit) throws MoeraNodeException {
        ReflectionTestUtils.setField(job, "parameters", new FetchRecommendationJob.Parameters(Feed.NEWS, limit));
        job.execute();
    }

    private void recommendations(RecommendedPostingInfo... recommendations) throws MoeraNodeException {
        when(search.getRecommendedPostings(eq(Feed.NEWS), isNull(), anyInt())).thenReturn(recommendations);
    }

    private List<String> pickedPostingIds(int count) {
        ArgumentCaptor<Pick> picks = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool, times(count)).pick(picks.capture());
        return picks.getAllValues().stream().map(Pick::getRemotePostingId).toList();
    }

    private static RecommendedPostingInfo posting(String id, Integer benefit) {
        var posting = new RecommendedPostingInfo();
        posting.setNodeName("author");
        posting.setPostingId(id);
        posting.setBenefit(benefit);
        return posting;
    }

}
