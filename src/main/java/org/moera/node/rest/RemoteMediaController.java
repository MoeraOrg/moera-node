package org.moera.node.rest;

import java.util.Collection;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.rest.task.RemoteMediaDownloadJob;
import org.moera.node.task.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/media/private/{id}")
@NoCache
public class RemoteMediaController {

    private static final Logger log = LoggerFactory.getLogger(RemoteMediaController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemoteMediaCacheRepository remoteMediaCacheRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private Jobs jobs;

    @PostMapping("/download")
    @Admin(Scope.REMOTE_DOWNLOAD_PRIVATE_MEDIA)
    @Entitled
    @Transactional
    public PrivateMediaFileInfo download(@PathVariable String nodeName, @PathVariable String id) {
        log.info(
            "POST /nodes/{nodeName}/media/private/{id}/download (nodeName = {}, id = {})",
            LogUtil.format(nodeName), LogUtil.format(id)
        );

        Collection<RemoteMediaCache> caches =
            remoteMediaCacheRepository.findByMedia(requestContext.nodeId(), nodeName, id);

        MediaFile mediaFile = caches.stream()
            .map(RemoteMediaCache::getMediaFile)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (mediaFile != null) {
            Collection<MediaFileOwner> owners =
                mediaFileOwnerRepository.findByAdminFile(requestContext.nodeId(), mediaFile.getId());
            if (!owners.isEmpty()) {
                return PrivateMediaFileInfoUtil.build(
                    owners.iterator().next(), null, config.getMedia().getDirectServe()
                );
            }
        }

        RemoteMediaError error = caches.stream()
            .map(RemoteMediaCache::getError)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (error != null) {
            throw new OperationFailure(error.getErrorCode());
        }

        boolean pending = jobs.isRunning(
            RemoteMediaDownloadJob.class,
            (nodeId, p) ->
                Objects.equals(nodeId, requestContext.nodeId())
                && Objects.equals(p.getNodeName(), nodeName)
                && Objects.equals(p.getId(), id)
        );
        if (!pending) {
            jobs.run(
                RemoteMediaDownloadJob.class,
                new RemoteMediaDownloadJob.Parameters(nodeName, id),
                requestContext.nodeId()
            );
        }

        throw new OperationFailure("media.download-pending");
    }

}
