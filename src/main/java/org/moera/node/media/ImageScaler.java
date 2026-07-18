package org.moera.node.media;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;

public class ImageScaler {

    private record Attempt(int width, int height, long fileSize) {
    }

    private static final int MAX_ITERATIONS = 3;
    private static final double TARGET_SIZE_TOLERANCE = 0.05;
    private static final int DIMENSION_TOLERANCE = 10;

    public static String downsize(
        File source, File target, String mimeType, Dimension dimension, long fileSize, long targetSize
    ) throws IOException {
        var targetFormat = MimeUtil.downsize(mimeType);
        if (targetFormat == null || targetSize <= 0) {
            return mimeType;
        }

        Attempt best = null;
        Attempt last = null;
        BufferedImage sourceImage = readSourceImage(source, mimeType);
        double scale = Math.min(1, Math.sqrt((double) targetSize / fileSize));
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Dimension resized = scaled(dimension, scale);
            writeResized(sourceImage, target, targetFormat, resized);

            last = new Attempt(resized.width, resized.height, Files.size(target.toPath()));
            best = betterAttempt(best, last, targetSize);
            if (isCloseEnough(last.fileSize(), targetSize) || resized.width == 1 && resized.height == 1) {
                break;
            }

            double correctedScale = Math.min(1, scale * Math.sqrt((double) targetSize / last.fileSize()));
            Dimension corrected = scaled(dimension, correctedScale);
            if (isCloseEnough(corrected, resized)) {
                break;
            }

            scale = correctedScale;
        }

        if (best != null && !best.equals(last)) {
            writeResized(
                sourceImage,
                target,
                targetFormat,
                new Dimension(best.width(), best.height())
            );
        }
        return targetFormat.mimeType();
    }

    private static BufferedImage readSourceImage(File source, String sourceMimeType) throws IOException {
        return ThumbnailUtil.thumbnailOf(source, sourceMimeType)
            .scale(1)
            .useExifOrientation(true)
            .asBufferedImage();
    }

    private static void writeResized(
        BufferedImage sourceImage,
        File target,
        MimeUtil.ThumbnailFormat targetFormat,
        Dimension targetDimension
    ) throws IOException {
        var builder = Thumbnails.of(sourceImage)
            .size(targetDimension.width, targetDimension.height)
            .scalingMode(ScalingMode.PROGRESSIVE_BILINEAR)
            .rendering(Rendering.QUALITY)
            .antialiasing(Antialiasing.ON);
        try (OutputStream out = Files.newOutputStream(target.toPath())) {
            ThumbnailUtil.toOutputStream(builder, out, targetFormat);
        }
    }

    private static boolean isCloseEnough(long fileSize, long targetSize) {
        return Math.abs(fileSize - targetSize) <= Math.round(targetSize * TARGET_SIZE_TOLERANCE);
    }

    private static boolean isCloseEnough(Dimension dimension, Dimension targetDimension) {
        return Math.abs(dimension.width - targetDimension.width) <= DIMENSION_TOLERANCE
            && Math.abs(dimension.height - targetDimension.height) <= DIMENSION_TOLERANCE;
    }

    private static Dimension scaled(Dimension original, double scale) {
        return new Dimension(
            Math.max(1, (int) Math.round(original.width * scale)),
            Math.max(1, (int) Math.round(original.height * scale))
        );
    }

    private static Attempt betterAttempt(Attempt best, Attempt attempt, long targetSize) {
        if (best == null) {
            return attempt;
        }

        long maxAcceptedSize = targetSize + Math.round(targetSize * TARGET_SIZE_TOLERANCE);
        boolean bestFits = best.fileSize() <= maxAcceptedSize;
        boolean attemptFits = attempt.fileSize() <= maxAcceptedSize;
        if (bestFits != attemptFits) {
            return attemptFits ? attempt : best;
        }

        long bestDistance = Math.abs(best.fileSize() - targetSize);
        long attemptDistance = Math.abs(attempt.fileSize() - targetSize);
        if (bestDistance != attemptDistance) {
            return attemptDistance < bestDistance ? attempt : best;
        }

        return attempt.fileSize() > best.fileSize() ? attempt : best;
    }

}
