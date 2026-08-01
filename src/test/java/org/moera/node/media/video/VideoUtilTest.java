package org.moera.node.media.video;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VideoUtilTest {

    private static final Path VIDEO_PATH = Path.of("video.mp4");

    @Test
    void parsesDimensionAndDuration() throws Exception {
        String output = """
            {
                "programs": [],
                "streams": [
                    {
                        "index": 0,
                        "codec_name": "h264",
                        "codec_type": "video",
                        "width": 1920,
                        "height": 1080
                    }
                ],
                "format": {
                    "duration": "12.345"
                }
            }
            """;

        var info = VideoUtil.parseProbeOutput(VIDEO_PATH, output);

        Assertions.assertEquals(1920, info.dimension().width);
        Assertions.assertEquals(1080, info.dimension().height);
        Assertions.assertEquals(12.345f, info.duration());
    }

    @Test
    void rejectsOutputWithoutVideoStream() {
        String output = """
            {
                "streams": [],
                "format": {
                    "duration": "12.345"
                }
            }
            """;

        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> VideoUtil.parseProbeOutput(VIDEO_PATH, output)
        );
    }

    @Test
    void rejectsOutputWithoutDuration() {
        String output = """
            {
                "streams": [
                    {
                        "width": 1920,
                        "height": 1080
                    }
                ],
                "format": {}
            }
            """;

        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> VideoUtil.parseProbeOutput(VIDEO_PATH, output)
        );
    }

    @Test
    void rejectsMalformedOutput() {
        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> VideoUtil.parseProbeOutput(VIDEO_PATH, "not JSON")
        );
    }

}
