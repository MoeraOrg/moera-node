package org.moera.node.media;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.springframework.test.util.ReflectionTestUtils;

public class MediaManagerTest {

    @Test
    void cachedRemoteMediaErrorIsThrown() {
        RemoteMediaCache cache = new RemoteMediaCache();
        cache.setError(RemoteMediaError.DOWNLOAD_FAILED);

        RemoteMediaCacheRepository remoteMediaCacheRepository = (RemoteMediaCacheRepository) Proxy.newProxyInstance(
            RemoteMediaCacheRepository.class.getClassLoader(),
            new Class<?>[] {RemoteMediaCacheRepository.class},
            (proxy, method, args) -> method.getName().equals("findByMediaWithoutNode") ? List.of(cache) : null
        );

        MediaManager mediaManager = new MediaManager();
        ReflectionTestUtils.setField(mediaManager, "remoteMediaCacheRepository", remoteMediaCacheRepository);

        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        info.setId("media-id");
        info.setHash("media-hash");

        MoeraNodeException exception = Assertions.assertThrows(
            MoeraNodeException.class,
            () -> mediaManager.downloadPrivateMediaForCaching("remote", null, info, 1024)
        );

        Assertions.assertTrue(exception.getMessage().contains(RemoteMediaError.DOWNLOAD_FAILED.getErrorCode()));
    }

}
