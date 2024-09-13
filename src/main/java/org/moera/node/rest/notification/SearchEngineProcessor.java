package org.moera.node.rest.notification;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.SearchEngine;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.SearchEngineClickedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

@NotificationProcessor
public class SearchEngineProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SearchEngineStatisticsRepository searchEngineStatisticsRepository;

    @NotificationMapping(NotificationType.SEARCH_ENGINE_CLICKED)
    @Transactional
    public void clicked(SearchEngineClickedNotification notification) {
        if (ObjectUtils.isEmpty(universalContext.nodeName())) {
            return;
        }

        if (ObjectUtils.isEmpty(notification.getHeading())) {
            return;
        }
        SearchEngine searchEngine = SearchEngine.forValue(notification.getSearchEngine());
        if (searchEngine == null) {
            return;
        }

        SearchEngineStatistics searchEngineStatistics = new SearchEngineStatistics();
        searchEngineStatistics.setId(UUID.randomUUID());
        searchEngineStatistics.setNodeName(notification.getSenderNodeName());
        searchEngineStatistics.setEngine(searchEngine);
        searchEngineStatistics.setOwnerName(universalContext.nodeName());
        searchEngineStatistics.setPostingId(notification.getPostingId());
        searchEngineStatistics.setCommentId(notification.getCommentId());
        searchEngineStatistics.setMediaId(notification.getMediaId());
        searchEngineStatistics.setClickedAt(Util.toTimestamp(notification.getClickedAt()));
        searchEngineStatistics.setHeading(notification.getHeading());

        searchEngineStatisticsRepository.save(searchEngineStatistics);
    }

}
