package org.moera.node.media;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;

public class ThumbnailUtil {

    public static Thumbnails.Builder<?> thumbnailOf(File file, String mimeType) throws IOException {
        if (mimeType.equals("image/webp")) {
            // WebP reader works only this way
            BufferedImage image = ImageIO.read(file);
            return Thumbnails.of(image);
        } else {
            return Thumbnails.of(file);
        }
    }

    public static void toOutputStream(
        Thumbnails.Builder<?> builder,
        OutputStream out,
        MimeUtil.ThumbnailFormat targetFormat
    ) throws IOException {
        toOutputStream(builder, out, targetFormat, JpegUtil.DEFAULT_JPEG_QUALITY);
    }

    public static void toOutputStream(
        Thumbnails.Builder<?> builder,
        OutputStream out,
        MimeUtil.ThumbnailFormat targetFormat,
        float targetJpegQuality
    ) throws IOException {
        if (targetFormat.isJpeg()) {
            JpegUtil.writeJpeg(builder.asBufferedImage(), out, targetJpegQuality);
        } else {
            builder.outputFormat(targetFormat.format()).toOutputStream(out);
        }
    }

}
