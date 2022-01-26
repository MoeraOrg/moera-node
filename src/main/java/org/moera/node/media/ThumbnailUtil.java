package org.moera.node.media;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

}
