package org.moera.node.media;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.node.data.MediaFile;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RemoteMediaCacheOperations {

    private static final Logger log = LoggerFactory.getLogger(RemoteMediaCacheOperations.class);

    private static final int REMOTE_MEDIA_CACHE_TTL = 60; // days
    private static final int REMOTE_MEDIA_CACHE_ERROR_TTL = 1; // hour

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RemoteMediaCacheRepository remoteMediaCacheRepository;

    @Inject
    private Transaction tx;

    public void store(UUID nodeId, String remoteNodeName, String remoteMediaId, byte[] digest, MediaFile mediaFile) {
        try {
            tx.executeWrite(() -> {
                var deadline = Timestamp.from(Instant.now().plus(REMOTE_MEDIA_CACHE_TTL, ChronoUnit.DAYS));

                var cached = nodeId != null
                    ? remoteMediaCacheRepository.findByMediaAndNode(nodeId, remoteNodeName, remoteMediaId)
                    : remoteMediaCacheRepository.findByMediaWithoutNode(remoteNodeName, remoteMediaId);
                if (!cached.isEmpty()) {
                    cached.forEach(cache -> {
                        cache.setDigest(digest);
                        cache.setError(null);
                        if (mediaFile != null) {
                            cache.setMediaFile(mediaFile);
                        }
                        cache.setDeadline(Util.latest(cache.getDeadline(), deadline));
                    });
                    return;
                }

                RemoteMediaCache cache = new RemoteMediaCache();
                cache.setId(UUID.randomUUID());
                cache.setNodeId(nodeId);
                cache.setRemoteNodeName(remoteNodeName);
                cache.setRemoteMediaId(remoteMediaId);
                cache.setDigest(digest);
                cache.setMediaFile(mediaFile);
                cache.setDeadline(deadline);
                remoteMediaCacheRepository.save(cache);
            });
        } catch (DataIntegrityViolationException e) {
            // already created in another thread, ignore
        } catch (Throwable e) {
            log.error("Unexpected error occured while creating RemoteMediaCache: {}", e.getMessage());
        }
    }

    public void error(UUID nodeId, String remoteNodeName, String remoteMediaId, RemoteMediaError error) {
        try {
            tx.executeWrite(() -> {
                var deadline = Timestamp.from(Instant.now().plus(REMOTE_MEDIA_CACHE_ERROR_TTL, ChronoUnit.HOURS));

                var cached = nodeId != null
                    ? remoteMediaCacheRepository.findByMediaAndNode(nodeId, remoteNodeName, remoteMediaId)
                    : remoteMediaCacheRepository.findByMediaWithoutNode(remoteNodeName, remoteMediaId);
                if (!cached.isEmpty()) {
                    return;
                }

                RemoteMediaCache cache = new RemoteMediaCache();
                cache.setId(UUID.randomUUID());
                cache.setNodeId(nodeId);
                cache.setRemoteNodeName(remoteNodeName);
                cache.setRemoteMediaId(remoteMediaId);
                cache.setError(error);
                cache.setDeadline(deadline);
                remoteMediaCacheRepository.save(cache);
            });
        } catch (DataIntegrityViolationException e) {
            // already created in another thread, ignore
        } catch (Throwable e) {
            log.error("Unexpected error occured while creating RemoteMediaCache: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpiredRemoteMedia() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging remote media cache");

            remoteMediaCacheRepository.deleteExpired(Util.now());
        }
    }

}
