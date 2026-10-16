package org.moera.node.operations;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.moera.node.data.Feed;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.option.Options;
import org.moera.node.rest.task.FetchRecommendationJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationOperationsTest {

    private final UUID nodeId = UUID.randomUUID();
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final Jobs jobs = mock(Jobs.class);
    private final UniversalContext universalContext = mock(UniversalContext.class);
    private final RecommendationOperations operations = new RecommendationOperations();

    @BeforeEach
    void setUp() {
        when(universalContext.nodeId()).thenReturn(nodeId);
        Domains domains = mock(Domains.class);
        when(domains.getWarmDomainNames()).thenReturn(List.of("test.example"));
        when(domains.getDomainNodeId("test.example")).thenReturn(nodeId);
        when(jobs.isReady()).thenReturn(true);
        ReflectionTestUtils.setField(operations, "requestCounter", new RequestCounter());
        ReflectionTestUtils.setField(operations, "universalContext", universalContext);
        ReflectionTestUtils.setField(operations, "domains", domains);
        ReflectionTestUtils.setField(operations, "storyRepository", storyRepository);
        ReflectionTestUtils.setField(operations, "tx", new Transaction());
        ReflectionTestUtils.setField(operations, "jobs", jobs);
    }

    @Test
    void checkRecommendationsSkipsUntilJobsAreReady() {
        configureNewsRecommendations();
        ReflectionTestUtils.setField(operations, "jobs", new Jobs());

        assertDoesNotThrow(operations::checkRecommendations);

        verifyNoInteractions(storyRepository);
    }

    @Test
    void checkRecommendationsFetchesWhenJobsAreReady() {
        configureNewsRecommendations();

        operations.checkRecommendations();

        verify(jobs).run(
            FetchRecommendationJob.class,
            new FetchRecommendationJob.Parameters(Feed.NEWS, 1),
            nodeId
        );
    }

    @ParameterizedTest
    @CsvSource({"0, 20", "10, 13", "20, 8", "25, 6", "30, 4", "40, 2", "49, 1", "50, 1", "100, 1"})
    void populateExploreDecreasesRecommendationLimitQuadratically(int feedSize, int expectedLimit) {
        when(storyRepository.countInFeed(nodeId, Feed.EXPLORE)).thenReturn(feedSize);

        operations.populateExplore();

        verify(jobs).run(
            FetchRecommendationJob.class,
            new FetchRecommendationJob.Parameters(Feed.EXPLORE, expectedLimit),
            nodeId
        );
    }

    @Test
    void populateExploreRecountsFeedOnEveryRun() {
        when(storyRepository.countInFeed(nodeId, Feed.EXPLORE)).thenReturn(0, 20, 50);

        operations.populateExplore();
        operations.populateExplore();
        operations.populateExplore();

        ArgumentCaptor<FetchRecommendationJob.Parameters> parameters =
            ArgumentCaptor.forClass(FetchRecommendationJob.Parameters.class);
        verify(jobs, times(3)).run(eq(FetchRecommendationJob.class), parameters.capture(), eq(nodeId));
        assertEquals(List.of(20, 8, 1), parameters.getAllValues().stream()
            .map(FetchRecommendationJob.Parameters::limit).toList());
    }

    private void configureNewsRecommendations() {
        Options options = mock(Options.class);
        when(options.getString("recommendations.frequency")).thenReturn("normal");
        when(options.getInt("recommendations.max-non-viewed-news")).thenReturn(10);
        when(universalContext.getOptions()).thenReturn(options);
    }

}
