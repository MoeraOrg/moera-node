package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SearchHistoryInfo;
import org.moera.lib.node.types.SearchHistoryText;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.SearchHistory;
import org.moera.node.data.SearchHistoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.model.SearchHistoryInfoUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/activity/search")
@NoCache
public class ActivitySearchController {

    private static final Logger log = LoggerFactory.getLogger(ActivitySearchController.class);

    private static final int MAX_RECORDS_PER_REQUEST = 100;
    private static final Duration HISTORY_TTL = Duration.ofDays(365);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SearchHistoryRepository searchHistoryRepository;

    @Inject
    private RequestCounter requestCounter;

    @GetMapping
    @Admin(Scope.OTHER)
    @Transactional
    public List<SearchHistoryInfo> getAll(
        @RequestParam(required = false) String prefix,
        @RequestParam(required = false) Integer limit
    ) {
        log.info("GET /activity/search (prefix = {}, limit = {})", LogUtil.format(prefix), LogUtil.format(limit));

        prefix = prefix != null ? prefix.trim() : "";
        limit = limit != null && limit <= MAX_RECORDS_PER_REQUEST ? limit : MAX_RECORDS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");

        return searchHistoryRepository.findByPrefix(requestContext.nodeId(), prefix, PageRequest.of(0, limit))
            .stream()
            .map(SearchHistoryInfoUtil::build)
            .toList();
    }

    @PostMapping
    @Admin(Scope.OTHER)
    @Transactional
    public SearchHistoryInfo post(@RequestBody SearchHistoryText historyText) {
        log.info("POST /activity/search (query = {})", LogUtil.format(historyText.getQuery(), 128));

        historyText.validate();

        String query = historyText.getQuery().trim();
        SearchHistory searchHistory = searchHistoryRepository.findByQuery(
            requestContext.nodeId(), query, PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);

        if (searchHistory == null) {
            searchHistory = new SearchHistory();
            searchHistory.setId(UUID.randomUUID());
            searchHistory.setNodeId(requestContext.nodeId());
        }
        searchHistory.setQuery(query);
        searchHistory.setCreatedAt(Util.now());
        searchHistory.setDeadline(Timestamp.from(Instant.now().plus(HISTORY_TTL)));
        searchHistory = searchHistoryRepository.save(searchHistory);

        return SearchHistoryInfoUtil.build(searchHistory);
    }

    @DeleteMapping
    @Admin(Scope.OTHER)
    @Transactional
    public Result delete(@RequestParam(required = false) String query) {
        log.info("DELETE /activity/search (query = {})", LogUtil.format(query));

        ValidationUtil.notBlank(query, "search-history.query.blank");

        searchHistoryRepository.deleteByQuery(requestContext.nodeId(), query);

        return Result.OK;
    }

    @Scheduled(fixedDelayString = "P7D")
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging old search history records");

            searchHistoryRepository.deleteExpired(Util.now());
        }
    }

}
