package org.moera.node.rest.notification;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.SearchEngineClickedNotification;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.global.UniversalContext;
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
        if (
            ObjectUtils.isEmpty(universalContext.nodeName())
            || ObjectUtils.isEmpty(notification.getHeading())
            || notification.getSearchEngine() == null
        ) {
            return;
        }

        SearchEngineStatistics searchEngineStatistics = new SearchEngineStatistics();
        searchEngineStatistics.setId(UUID.randomUUID());
        searchEngineStatistics.setNodeName(notification.getSenderNodeName());
        searchEngineStatistics.setEngine(notification.getSearchEngine());
        searchEngineStatistics.setOwnerName(universalContext.nodeName());
        searchEngineStatistics.setPostingId(notification.getPostingId());
        searchEngineStatistics.setCommentId(notification.getCommentId());
        searchEngineStatistics.setMediaId(notification.getMediaId());
        searchEngineStatistics.setClickedAt(Util.toTimestamp(notification.getClickedAt()));
        searchEngineStatistics.setHeading(notification.getHeading());

        searchEngineStatisticsRepository.save(searchEngineStatistics);
    }

}
