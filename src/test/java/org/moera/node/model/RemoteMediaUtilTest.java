package org.moera.node.model;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;

public class RemoteMediaUtilTest {

    @Test
    void durationIsIncludedInRemoteMedia() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMimeType("video/mp4");
        mediaFile.setDuration(12.345f);
        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setMediaFile(mediaFile);

        RemoteMedia remoteMedia = new RemoteMedia();
        RemoteMediaUtil.toRemoteMedia(remoteMedia, mediaFileOwner, UUID.randomUUID());

        Assertions.assertEquals(12.345f, remoteMedia.getDuration());
    }

    @Test
    void durationIsStoredFromRemoteMedia() {
        RemoteMedia remoteMedia = new RemoteMedia();
        remoteMedia.setDuration(12.345f);

        RemoteMediaFile remoteMediaFile = RemoteMediaUtil.toNewRemoteMediaFile(UUID.randomUUID(), remoteMedia);

        Assertions.assertEquals(12.345f, remoteMediaFile.getDuration());
    }

    @Test
    void durationIsIncludedInRemoteMediaInfo() {
        RemoteMediaFile remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setId(UUID.randomUUID());
        remoteMediaFile.setDuration(12.345f);

        Assertions.assertEquals(12.345f, RemoteMediaInfoUtil.build(remoteMediaFile, null).getDuration());
    }

}
