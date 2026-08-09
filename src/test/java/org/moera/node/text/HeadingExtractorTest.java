package org.moera.node.text;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.media.LocalRemoteMedia;

public class HeadingExtractorTest {

    @Test
    void recognizedTextIsTakenFromRemoteMedia() {
        RemoteMediaFile remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setMimeType("image/jpeg");
        remoteMediaFile.setRecognizedText("recognized remote text");

        String heading = HeadingExtractor.extractHeading(
            new Body(), List.of(new LocalRemoteMedia(null, remoteMediaFile)), false
        );

        Assertions.assertEquals("recognized remote text", heading);
    }

    @Test
    void localMediaTakesPrecedenceOverRemoteMedia() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("local-hash");
        mediaFile.setMimeType("image/jpeg");
        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setMediaFile(mediaFile);

        RemoteMediaFile remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setMimeType("image/jpeg");
        remoteMediaFile.setRecognizedText("recognized remote text");

        String heading = HeadingExtractor.extractHeading(
            new Body(), List.of(new LocalRemoteMedia(mediaFileOwner, remoteMediaFile)), false
        );

        Assertions.assertEquals(HeadingExtractor.EMOJI_PICTURE, heading);
    }

}
