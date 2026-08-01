package org.moera.node.media.video;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.moera.lib.util.LogUtil;
import org.moera.node.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class VideoUtil {

    private static final Logger log = LoggerFactory.getLogger(VideoUtil.class);

    private static final int PROBE_TIMEOUT_SEC = 10;
    private static final int THUMBNAIL_TIMEOUT_SEC = 15;

    public record VideoInfo(Dimension dimension, Float duration) {
    }

    public static VideoInfo getVideoInfo(Path path) throws IOException {
        var processBuilder = new ProcessBuilder(
            "ffprobe",
            "-select_streams", "V",
            "-show_entries", "format=duration",
            "-show_entries", "stream=width,height",
            "-of", "json",
            path.toString()
        );

        var result = runTool(processBuilder, PROBE_TIMEOUT_SEC, "Video probing was interrupted", path);

        return parseProbeOutput(path, result.stdout());
    }

    private record ProbeFormat(String duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeStream(Integer width, Integer height) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeInfo(ProbeFormat format, List<ProbeStream> streams) {
    }

    static VideoInfo parseProbeOutput(Path path, String output) throws InvalidVideoException {
        try {
            ProbeInfo probe = new ObjectMapper().readValue(output, ProbeInfo.class);
            ProbeStream stream = probe.streams() != null && !probe.streams().isEmpty()
                ? probe.streams().getFirst()
                : null;
            Float duration = probe.format() != null && probe.format().duration() != null
                ? Float.valueOf(probe.format().duration())
                : null;
            if (
                stream == null
                || stream.width() == null
                || stream.width() <= 0
                || stream.height() == null
                || stream.height() <= 0
                || duration == null
                || !Float.isFinite(duration)
                || duration < 0
            ) {
                throw new InvalidVideoException();
            }
            return new VideoInfo(new Dimension(stream.width(), stream.height()), duration);
        } catch (InvalidVideoException e) {
            throw e;
        } catch (Exception e) {
            log.warn(
                "Error parsing video file information for {}: {}",
                LogUtil.format(path.toString()), e.getMessage()
            );
            throw new InvalidVideoException();
        }
    }

    public static void thumbnailVideo(Path path, int width, Path thumbnailPath) throws IOException {
        var processBuilder = new ProcessBuilder(
            "ffmpeg",
            "-hide_banner",
            "-loglevel", "error",
            "-ss", "1",
            "-noaccurate_seek",
            "-i", path.toString(),
            "-map", "0:V:0",
            "-an",
            "-sn",
            "-dn",
            "-frames:v", "1",
            "-vf", "scale=%d:%d:force_original_aspect_ratio=decrease".formatted(width, width),
            "-f", "singlejpeg",
            "-q:v", "3",
            "-y",
            thumbnailPath.toString()
        );

        runTool(processBuilder, THUMBNAIL_TIMEOUT_SEC, "Creating a video thumbnail was interrupted", path);
    }

    private static ToolRunner.ToolResult runTool(
        ProcessBuilder processBuilder,
        int timeoutSec,
        String interruptMessage,
        Path inputPath
    ) throws IOException {
        ToolRunner.ToolResult result;
        try {
            result = ToolRunner.run(processBuilder, timeoutSec);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(interruptMessage, e);
        }

        if (result.timeout() || result.exitCode() != 0) {
            log.warn(
                "Error reading video file {}: {}",
                LogUtil.format(inputPath.toString()), LogUtil.format(result.stderr())
            );
            throw new InvalidVideoException();
        }

        return result;
    }

}
