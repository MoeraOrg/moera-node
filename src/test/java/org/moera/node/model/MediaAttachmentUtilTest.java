package org.moera.node.model;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;

public class MediaAttachmentUtilTest {

    @Test
    void durationIsIncludedForDownloadedRemoteMedia() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("hash");
        mediaFile.setMimeType("video/mp4");
        mediaFile.setDuration(12.345f);
        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setMediaFile(mediaFile);
        mediaFileOwner.setMalwareMarks("test");

        RemoteMediaFile remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setId(UUID.randomUUID());

        EntryAttachment attachment = new EntryAttachment(null, mediaFileOwner, remoteMediaFile, 0);

        var info = MediaAttachmentUtil.build(attachment, null, null);

        Assertions.assertEquals(12.345f, info.getMedia().getDuration());
        Assertions.assertNull(info.getRemoteMedia().getDuration());
    }

}
