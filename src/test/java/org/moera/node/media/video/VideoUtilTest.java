package org.moera.node.media.video;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VideoUtilTest {

    private static final Path VIDEO_PATH = Path.of("video.mp4");

    @Test
    void parsesAndCompactsProbeOutput() throws Exception {
        String output = probe(
            "mov,mp4,m4a,3gp,3g2,mj2",
            video(0, 1280, 720, "h264", "yuv420p", "30000/1001", false, null),
            audio(1, "aac", 2, "128000")
        );

        var info = VideoProbe.parseProbeOutput(VIDEO_PATH, "video/mp4", output);

        Assertions.assertEquals(1280, info.dimension().width);
        Assertions.assertEquals(720, info.dimension().height);
        Assertions.assertEquals(12.345f, info.duration());
        Assertions.assertEquals(output.replaceAll("\\s+", ""), info.streamInfo());
        Assertions.assertFalse(info.uncompressed());
        Assertions.assertEquals(0, info.videoStreamIndex());
        Assertions.assertEquals(1, info.audioStreamIndex());
    }

    @Test
    void silentVideoAndAncillaryStreamsAreAlreadyCompressed() throws Exception {
        String output = probe(
            "mp4",
            video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null),
            subtitle(2, "subrip"),
            video(3, 600, 600, "mjpeg", "yuvj420p", "0/0", true, null)
        );

        var info = VideoProbe.parseProbeOutput(VIDEO_PATH, "video/mp4", output);

        Assertions.assertFalse(info.uncompressed());
        Assertions.assertNull(info.audioStreamIndex());
        Assertions.assertEquals(List.of(2), info.subtitleStreamIndexes());
        Assertions.assertEquals(List.of(3), info.attachedPictureStreamIndexes());
    }

    @Test
    void dataAndMultiplePrimaryOrAudioStreamsForceCompression() throws Exception {
        String primary = video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null);
        Assertions.assertTrue(info(probe("mp4", primary, data(1))).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", primary, video(2, 640, 360, "h264", "yuv420p", "25/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", primary, audio(1, "aac", 2, "64000"), audio(2, "aac", 1, "32000")
        )).uncompressed());
    }

    @Test
    void audioBitrateBoundaryAndMissingBitrateAreClassifiedCorrectly() throws Exception {
        Assertions.assertFalse(infoWithAudio("143999", 2, "aac").uncompressed());
        Assertions.assertFalse(infoWithAudio("144000", 2, "aac").uncompressed());
        Assertions.assertFalse(infoWithAudio(null, 2, "aac").uncompressed());
        Assertions.assertTrue(infoWithAudio("144001", 2, "aac").uncompressed());
        Assertions.assertTrue(infoWithAudio("not-a-number", 2, "aac").uncompressed());
        Assertions.assertTrue(infoWithAudio("144000", 3, "aac").uncompressed());
        Assertions.assertTrue(infoWithAudio("144000", 2, "mp3").uncompressed());
    }

    @Test
    void displayedRotationControlsPortraitBounds() throws Exception {
        var info = info(probe(
            "mp4", video(0, 1280, 720, "h264", "yuv420p", "30/1", false, 90)
        ));

        Assertions.assertEquals(720, info.dimension().width);
        Assertions.assertEquals(1280, info.dimension().height);
        Assertions.assertEquals(90, info.rotation());
        Assertions.assertFalse(info.uncompressed());

        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1282, 720, "h264", "yuv420p", "30/1", false, 90)
        )).uncompressed());
    }

    @Test
    void landscapePortraitAndSquareBoundsAreOrientationAware() throws Exception {
        Assertions.assertFalse(info(probe(
            "mp4", video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1281, 720, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertFalse(info(probe(
            "mp4", video(0, 720, 1280, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 720, 1281, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertFalse(info(probe(
            "mp4", video(0, 720, 720, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 721, 721, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
    }

    @Test
    void codecPixelFormatFrameRateAndContainerAreConservative() throws Exception {
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1280, 720, "hevc", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1280, 720, "h264", "yuv422p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1280, 720, "h264", "yuv420p", "30001/1000", false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "mp4", video(0, 1280, 720, "h264", "yuv420p", null, false, null)
        )).uncompressed());
        Assertions.assertTrue(info(probe(
            "matroska", video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null)
        )).uncompressed());
        Assertions.assertTrue(VideoProbe.parseProbeOutput(
            VIDEO_PATH,
            "video/webm",
            probe("mp4", video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null))
        ).uncompressed());
    }

    @Test
    void compressionCommandMapsProfileAndOmitsData() throws Exception {
        var info = info(probe(
            "matroska",
            video(4, 1920, 1080, "hevc", "yuv420p", "60/1", false, null),
            audio(6, "opus", 6, "256000"),
            subtitle(7, "subrip"),
            video(8, 600, 600, "mjpeg", "yuvj420p", "0/0", true, null),
            data(9)
        ));

        List<String> command = VideoUtil.compressionProcess(
            Path.of("input"), Path.of("output"), info, true, true
        ).command();

        assertOption(command, "-map", "0:4");
        assertOption(command, "-map", "0:6");
        assertOption(command, "-map", "0:7?");
        assertOption(command, "-map", "0:8?");
        Assertions.assertFalse(command.contains("0:9"));
        assertOption(command, "-c:v:0", "libx264");
        assertOption(command, "-crf", "23");
        assertOption(command, "-preset", "medium");
        assertOption(command, "-pix_fmt:v:0", "yuv420p");
        assertOption(command, "-fpsmax:v:0", "30");
        assertOption(command, "-c:a:0", "aac");
        assertOption(command, "-b:a:0", "128k");
        assertOption(command, "-ac:a:0", "2");
        assertOption(command, "-c:s", "mov_text");
        assertOption(command, "-c:v:1", "copy");
        assertOption(command, "-map_metadata", "0");
        assertOption(command, "-map_chapters", "-1");
        assertOption(command, "-movflags", "+faststart");
        assertOption(command, "-progress", "pipe:1");
        assertOption(command, "-stats_period", "5");
        assertOption(command, "-f", "mp4");
        Assertions.assertTrue(command.contains("-noautorotate"));
        Assertions.assertTrue(option(command, "-filter:v:0").contains("min(iw,1280)"));
        Assertions.assertTrue(option(command, "-filter:v:0").contains("min(ih,720)"));
    }

    @Test
    void compressionCommandUsesDisplayedOrientationWithoutUpscaling() throws Exception {
        var portrait = info(probe(
            "matroska", video(0, 1080, 1920, "hevc", "yuv420p", "24/1", false, null)
        ));
        String portraitFilter = option(
            VideoUtil.compressionProcess(
                Path.of("in"), Path.of("out"), portrait, false, false
            ).command(),
            "-filter:v:0"
        );
        Assertions.assertTrue(portraitFilter.contains("min(iw,720)"));
        Assertions.assertTrue(portraitFilter.contains("min(ih,1280)"));
        Assertions.assertTrue(portraitFilter.contains("force_original_aspect_ratio=decrease"));

        var rotatedPortrait = info(probe(
            "matroska", video(0, 1920, 1080, "hevc", "yuv420p", "24/1", false, 90)
        ));
        String rotatedFilter = option(
            VideoUtil.compressionProcess(
                Path.of("in"), Path.of("out"), rotatedPortrait, false, false
            ).command(),
            "-filter:v:0"
        );
        Assertions.assertTrue(rotatedFilter.contains("min(iw,1280)"));
        Assertions.assertTrue(rotatedFilter.contains("min(ih,720)"));
    }

    @Test
    void rejectsMissingOrMalformedRequiredProbeInformation() {
        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> parseProbeOutput("{\"streams\":[],\"format\":{\"duration\":\"1\"}}")
        );
        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> parseProbeOutput(
                probe(null, video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null))
                    .replace("\"duration\": \"12.345\"", "")
            )
        );
        Assertions.assertThrows(
            InvalidVideoException.class,
            () -> parseProbeOutput("not JSON")
        );
    }

    private static VideoInfo parseProbeOutput(String output) throws InvalidVideoException {
        return VideoProbe.parseProbeOutput(VIDEO_PATH, "video/mp4", output);
    }

    private static VideoInfo info(String output) throws InvalidVideoException {
        return parseProbeOutput(output);
    }

    private static VideoInfo infoWithAudio(String bitrate, int channels, String codec)
        throws InvalidVideoException {
        return info(probe(
            "mp4",
            video(0, 1280, 720, "h264", "yuv420p", "30/1", false, null),
            audio(1, codec, channels, bitrate)
        ));
    }

    private static String probe(String formatName, String... streams) {
        String formatNameField = formatName != null ? "\"format_name\": \"" + formatName + "\"," : "";
        return """
            {
              "streams": [%s],
              "format": {%s "duration": "12.345"}
            }
            """.formatted(String.join(",", streams), formatNameField);
    }

    private static String video(
        int index,
        int width,
        int height,
        String codec,
        String pixelFormat,
        String frameRate,
        boolean attachedPicture,
        Integer rotation
    ) {
        String rate = frameRate != null
            ? "\"avg_frame_rate\": \"" + frameRate + "\", \"r_frame_rate\": \"" + frameRate + "\","
            : "";
        String sideData = rotation != null ? ", \"side_data_list\": [{\"rotation\": " + rotation + "}]" : "";
        return """
            {"index": %d, "codec_name": "%s", "codec_type": "video", "width": %d, "height": %d,
             "pix_fmt": "%s", %s "disposition": {"default": 1, "attached_pic": %d}%s}
            """.formatted(index, codec, width, height, pixelFormat, rate, attachedPicture ? 1 : 0, sideData);
    }

    private static String audio(int index, String codec, int channels, String bitrate) {
        String bitrateField = bitrate != null ? ", \"bit_rate\": \"" + bitrate + "\"" : "";
        return """
            {"index": %d, "codec_name": "%s", "codec_type": "audio", "channels": %d%s,
             "disposition": {"default": 1}}
            """.formatted(index, codec, channels, bitrateField);
    }

    private static String subtitle(int index, String codec) {
        return """
            {"index": %d, "codec_name": "%s", "codec_type": "subtitle", "disposition": {"default": 1}}
            """.formatted(index, codec);
    }

    private static String data(int index) {
        return "{\"index\": %d, \"codec_type\": \"data\"}".formatted(index);
    }

    private static void assertOption(List<String> command, String option, String value) {
        for (int index = 0; index < command.size() - 1; index++) {
            if (command.get(index).equals(option) && command.get(index + 1).equals(value)) {
                return;
            }
        }
        Assertions.fail("Missing command option %s %s".formatted(option, value));
    }

    private static String option(List<String> command, String option) {
        int index = command.indexOf(option);
        Assertions.assertTrue(index >= 0, "Missing command option " + option);
        return command.get(index + 1);
    }

}
