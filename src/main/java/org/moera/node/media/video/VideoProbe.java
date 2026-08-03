package org.moera.node.media.video;

import java.awt.Dimension;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.moera.lib.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class VideoProbe {

    private static final Logger log = LoggerFactory.getLogger(VideoProbe.class);

    private static final double MAX_FRAME_RATE = 30;
    private static final long MAX_AUDIO_BIT_RATE = 131_072;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeFormat(@JsonProperty("format_name") String formatName, String duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeDisposition(
        @JsonProperty("default") Integer defaultStream,
        @JsonProperty("attached_pic")
        Integer attachedPic
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeTags(String rotate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeSideData(Integer rotation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeStream(
        Integer index,
        @JsonProperty("codec_name")
        String codecName,
        @JsonProperty("codec_type")
        String codecType,
        Integer width,
        Integer height,
        @JsonProperty("pix_fmt")
        String pixFmt,
        @JsonProperty("avg_frame_rate")
        String avgFrameRate,
        @JsonProperty("r_frame_rate")
        String rFrameRate,
        @JsonProperty("bit_rate")
        String bitRate,
        Integer channels,
        ProbeDisposition disposition,
        ProbeTags tags,
        @JsonProperty("side_data_list")
        List<ProbeSideData> sideDataList
    ) {

        boolean isDefault() {
            return disposition != null && Integer.valueOf(1).equals(disposition.defaultStream());
        }

        boolean isAttachedPicture() {
            return disposition != null && Integer.valueOf(1).equals(disposition.attachedPic());
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProbeInfo(ProbeFormat format, List<ProbeStream> streams) {
    }

    static VideoInfo parseProbeOutput(Path path, String contentType, String output) throws InvalidVideoException {
        try {
            var objectMapper = new ObjectMapper();
            var probeJson = objectMapper.readTree(output);
            ProbeInfo probe = objectMapper.treeToValue(probeJson, ProbeInfo.class);
            List<ProbeStream> streams = probe.streams() != null ? probe.streams() : List.of();
            List<ProbeStream> videos = streams.stream()
                .filter(stream -> "video".equals(stream.codecType()))
                .filter(stream -> !stream.isAttachedPicture())
                .toList();
            ProbeStream video = preferred(videos);
            Float duration = parseDuration(probe.format());
            if (
                video == null
                || video.index() == null
                || video.width() == null
                || video.width() <= 0
                || video.height() == null
                || video.height() <= 0
                || duration == null
            ) {
                throw new InvalidVideoException();
            }

            int rotation = rotation(video);
            Dimension dimension = displayedDimension(video, rotation);
            List<ProbeStream> audio = streams.stream()
                .filter(stream -> "audio".equals(stream.codecType()))
                .toList();
            ProbeStream selectedAudio = preferred(audio);
            if (selectedAudio != null && selectedAudio.index() == null) {
                throw new InvalidVideoException();
            }
            List<Integer> subtitles = streams.stream()
                .filter(stream -> "subtitle".equals(stream.codecType()))
                .map(ProbeStream::index)
                .filter(Objects::nonNull)
                .toList();
            List<Integer> attachedPictures = streams.stream()
                .filter(stream -> "video".equals(stream.codecType()) && stream.isAttachedPicture())
                .map(ProbeStream::index)
                .filter(Objects::nonNull)
                .toList();

            boolean compressed = "video/mp4".equalsIgnoreCase(contentType)
                && isMp4(probe.format())
                && videos.size() == 1
                && "h264".equals(video.codecName())
                && "yuv420p".equals(video.pixFmt())
                && fits720p(dimension)
                && validFrameRate(video)
                && audio.size() <= 1
                && validAudio(selectedAudio)
                && streams.stream().noneMatch(stream -> "data".equals(stream.codecType()));

            return new VideoInfo(
                dimension,
                duration,
                objectMapper.writeValueAsString(probeJson),
                !compressed,
                video.index(),
                selectedAudio != null ? selectedAudio.index() : null,
                subtitles,
                attachedPictures,
                selectedAudio != null ? selectedAudio.channels() : null,
                rotation
            );
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

    private static ProbeStream preferred(List<ProbeStream> streams) {
        return streams.stream()
            .max(Comparator.comparing(ProbeStream::isDefault))
            .orElse(null);
    }

    private static Float parseDuration(ProbeFormat format) {
        if (format == null || format.duration() == null) {
            return null;
        }
        float duration = Float.parseFloat(format.duration());
        return Float.isFinite(duration) && duration >= 0 ? duration : null;
    }

    private static int rotation(ProbeStream stream) {
        if (stream.sideDataList() != null) {
            for (ProbeSideData sideData : stream.sideDataList()) {
                if (sideData.rotation() != null) {
                    return normalizeRotation(sideData.rotation());
                }
            }
        }
        if (stream.tags() != null && stream.tags().rotate() != null) {
            return normalizeRotation(Integer.parseInt(stream.tags().rotate()));
        }
        return 0;
    }

    private static int normalizeRotation(int rotation) {
        return Math.floorMod(rotation, 360);
    }

    private static Dimension displayedDimension(ProbeStream stream, int rotation) {
        return rotation == 90 || rotation == 270
            ? new Dimension(stream.height(), stream.width())
            : new Dimension(stream.width(), stream.height());
    }

    private static boolean isMp4(ProbeFormat format) {
        if (format == null || format.formatName() == null) {
            return false;
        }
        for (String name : format.formatName().split(",")) {
            if ("mp4".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fits720p(Dimension dimension) {
        return dimension.width >= dimension.height
            ? dimension.width <= 1280 && dimension.height <= 720
            : dimension.width <= 720 && dimension.height <= 1280;
    }

    private static boolean validFrameRate(ProbeStream video) {
        Double rate = rational(video.avgFrameRate());
        if (rate == null || rate <= 0) {
            rate = rational(video.rFrameRate());
        }
        return rate != null && rate > 0 && rate <= MAX_FRAME_RATE;
    }

    private static Double rational(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("/", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            double result = numerator / denominator;
            return Double.isFinite(result) ? result : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean validAudio(ProbeStream audio) {
        if (audio == null) {
            return true;
        }
        if (
            !"aac".equals(audio.codecName())
            || audio.channels() == null
            || audio.channels() <= 0
            || audio.channels() > 2
        ) {
            return false;
        }
        if (audio.bitRate() == null) {
            return true;
        }
        try {
            long bitRate = Long.parseLong(audio.bitRate());
            return bitRate >= 0 && bitRate <= MAX_AUDIO_BIT_RATE;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
