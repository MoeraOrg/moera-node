package org.moera.node.media.video;

import java.awt.Dimension;
import java.util.List;

public record VideoInfo(
    Dimension dimension,
    Float duration,
    String streamInfo,
    boolean uncompressed,
    int videoStreamIndex,
    Integer audioStreamIndex,
    List<Integer> subtitleStreamIndexes,
    List<Integer> attachedPictureStreamIndexes,
    Integer audioChannels,
    int rotation
) {
}
