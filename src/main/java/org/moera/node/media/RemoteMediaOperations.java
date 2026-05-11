package org.moera.node.media;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RemoteMediaOperations {

    private static final Logger log = LoggerFactory.getLogger(RemoteMediaOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private RemoteMediaFileRepository remoteMediaFileRepository;

    public RemoteMediaFile store(String remoteNodeName, RemoteMedia media) {
        RemoteMediaFile remoteMediaFile = create(remoteNodeName, media.getId());
        remoteMediaFile.setHash(media.getHash());
        remoteMediaFile.setDigest(Util.base64decode(media.getDigest()));
        remoteMediaFile.setMimeType(media.getMimeType());
        remoteMediaFile.setAttachment(Boolean.TRUE.equals(media.getAttachment()));
        return remoteMediaFile;
    }

    public RemoteMediaFile store(String remoteNodeName, PrivateMediaFileInfo media) {
        RemoteMediaFile remoteMediaFile = create(remoteNodeName, media.getId());
        remoteMediaFile.setHash(media.getHash());
        remoteMediaFile.setMimeType(media.getMimeType());
        remoteMediaFile.setAttachment(Boolean.TRUE.equals(media.getAttachment()));
        remoteMediaFile.setSizeX(media.getWidth());
        remoteMediaFile.setSizeY(media.getHeight());
        remoteMediaFile.setFileSize(media.getSize());
        return remoteMediaFile;
    }

    private RemoteMediaFile create(String remoteNodeName, String remoteMediaId) {
        RemoteMediaFile remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setId(UUID.randomUUID());
        remoteMediaFile.setNodeId(universalContext.nodeId());
        remoteMediaFile.setNodeName(remoteNodeName);
        remoteMediaFile.setMediaId(remoteMediaId);
        return remoteMediaFileRepository.save(remoteMediaFile);
    }

    @Scheduled(fixedDelayString = "PT6H")
    @Transactional
    public void purgeUnused() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging unused remote media files");

            remoteMediaFileRepository.deleteUnused(Util.now());
        }
    }

}
