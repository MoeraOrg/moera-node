package org.moera.node.media.video;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoUtil {

    private static final Logger log = LoggerFactory.getLogger(VideoUtil.class);

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration THUMBNAIL_TIMEOUT = Duration.ofSeconds(15);

    public static VideoInfo getVideoInfo(Path path, String contentType) throws IOException {
        var processBuilder = new ProcessBuilder(
            "ffprobe",
            "-v", "error",
            "-show_streams",
            "-show_format",
            "-of", "json",
            path.toString()
        );

        var result = runTool(processBuilder, PROBE_TIMEOUT, "Video probing was interrupted", path);

        return VideoProbe.parseProbeOutput(path, contentType, result.stdout());
    }

    static ProcessBuilder compressionProcess(
        Path input,
        Path output,
        VideoInfo info,
        boolean preserveSubtitles,
        boolean preserveAttachedPictures
    ) {
        List<String> command = new ArrayList<>(List.of(
            "ffmpeg",
            "-hide_banner",
            "-loglevel", "warning",
            "-noautorotate",
            "-i", input.toString(),
            "-map_metadata", "0",
            "-map_chapters", "-1",
            "-map", "0:" + info.videoStreamIndex()
        ));
        if (info.audioStreamIndex() != null) {
            command.addAll(List.of("-map", "0:" + info.audioStreamIndex()));
        }
        if (preserveSubtitles) {
            for (Integer index : info.subtitleStreamIndexes()) {
                command.addAll(List.of("-map", "0:" + index + "?"));
            }
        }
        if (preserveAttachedPictures) {
            for (Integer index : info.attachedPictureStreamIndexes()) {
                command.addAll(List.of("-map", "0:" + index + "?"));
            }
        }

        boolean rotated = info.rotation() == 90 || info.rotation() == 270;
        boolean portrait = info.dimension().height > info.dimension().width;
        int displayWidth = portrait ? 720 : 1280;
        int displayHeight = portrait ? 1280 : 720;
        int codedWidth = rotated ? displayHeight : displayWidth;
        int codedHeight = rotated ? displayWidth : displayHeight;
        String videoFilter = "scale=w='min(iw,%d)':h='min(ih,%d)'"
            + ":force_original_aspect_ratio=decrease:force_divisible_by=2";
        videoFilter = videoFilter.formatted(codedWidth, codedHeight);

        command.addAll(List.of(
            "-c:v:0", "libx264",
            "-crf", "23",
            "-preset", "medium",
            "-pix_fmt:v:0", "yuv420p",
            "-filter:v:0", videoFilter,
            "-fpsmax:v:0", "30"
        ));

        if (info.audioStreamIndex() != null) {
            command.addAll(List.of("-c:a:0", "aac", "-b:a:0", "128k"));
            if (info.audioChannels() == null || info.audioChannels() > 2) {
                command.addAll(List.of("-ac:a:0", "2"));
            }
        }
        if (preserveSubtitles && !info.subtitleStreamIndexes().isEmpty()) {
            command.addAll(List.of("-c:s", "mov_text"));
        }
        if (preserveAttachedPictures) {
            for (int i = 1; i <= info.attachedPictureStreamIndexes().size(); i++) {
                command.addAll(List.of("-c:v:" + i, "copy"));
            }
        }

        command.addAll(List.of(
            "-movflags", "+faststart",
            "-progress", "pipe:1",
            "-stats_period", "5",
            "-nostats",
            "-f", "mp4",
            "-y",
            output.toString()
        ));

        return new ProcessBuilder(command);
    }

    public static ToolRunner.ToolResult compressVideo(
        Path input,
        Path output,
        VideoInfo info,
        boolean preserveSubtitles,
        boolean preserveAttachedPictures,
        Duration noProgressTimeout,
        Duration totalTimeout
    ) throws IOException, InterruptedException {
        return ToolRunner.runWithProgress(
            VideoUtil.compressionProcess(input, output, info, preserveSubtitles, preserveAttachedPictures),
            noProgressTimeout,
            totalTimeout
        );
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
            "-f", "image2",
            "-update", "1",
            "-c:v", "mjpeg",
            "-q:v", "3",
            "-y",
            thumbnailPath.toString()
        );

        runTool(processBuilder, THUMBNAIL_TIMEOUT, "Creating a video thumbnail was interrupted", path);
    }

    private static ToolRunner.ToolResult runTool(
        ProcessBuilder processBuilder,
        Duration timeout,
        String interruptMessage,
        Path inputPath
    ) throws IOException {
        ToolRunner.ToolResult result;
        try {
            result = ToolRunner.run(processBuilder, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(interruptMessage, e);
        }

        if (result.stopReason() != ToolRunner.StopReason.NONE || result.exitCode() != 0) {
            log.warn(
                "Error reading video file {}: {}",
                LogUtil.format(inputPath.toString()), LogUtil.format(result.stderr())
            );
            throw new InvalidVideoException();
        }

        return result;
    }

}
