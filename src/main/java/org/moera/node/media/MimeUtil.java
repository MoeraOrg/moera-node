package org.moera.node.media;

import java.util.HashMap;
import java.util.Map;

import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.util.ObjectUtils;

public class MimeUtil {

    public record ThumbnailFormat(String mimeType, String format) {

        public boolean isJpeg() {
            return MimeUtil.isJpeg(mimeType);
        }

    }

    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();
    private static final Map<String, String> ADDITIONAL_MIME_TYPES = Map.of(
        "text/markdown", "md"
    );
    private static final ThumbnailFormat LOSSY = new ThumbnailFormat("image/jpeg", "JPEG");
    private static final ThumbnailFormat LOSSLESS = new ThumbnailFormat("image/png", "PNG");
    private static final Map<String, ThumbnailFormat> THUMBNAIL_FORMATS = new HashMap<>();

    static {
        THUMBNAIL_FORMATS.put("image/avif", LOSSY);
        THUMBNAIL_FORMATS.put("image/gif", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/jp2", LOSSY);
        THUMBNAIL_FORMATS.put("image/jpeg", LOSSY);
        THUMBNAIL_FORMATS.put("image/pcx", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/pjpeg", LOSSY);
        THUMBNAIL_FORMATS.put("image/png", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-png", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/svg+xml", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/tiff", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/vnd.microsoft.icon", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/vnd.wap.wbmp", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/webp", LOSSY);
        THUMBNAIL_FORMATS.put("image/x-ms-bmp", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-portable-anymap", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-portable-bitmap", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-portable-graymap", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-portable-pixmap", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-xbitmap", LOSSLESS);
        THUMBNAIL_FORMATS.put("image/x-xpixmap", LOSSLESS);
    }

    public static String extension(String mimeType) {
        String extension = null;
        try {
            extension = MIME_TYPES.forName(mimeType).getExtension();
        } catch (MimeTypeException e) {
            // ignore
        }
        if (ObjectUtils.isEmpty(extension)) {
            extension = ADDITIONAL_MIME_TYPES.get(mimeType);
        }
        if (ObjectUtils.isEmpty(extension)) {
            extension = mimeType != null && mimeType.startsWith("text/") ? "txt" : "bin";
        }
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }

    public static String fileName(String name, String mimeType) {
        return name + "." + extension(mimeType);
    }

    public static boolean isSupportedImage(String mimeType) {
        return THUMBNAIL_FORMATS.containsKey(mimeType);
    }

    public static boolean isLossyImage(String mimeType) {
        var format = thumbnail(mimeType);
        return format != null && format.isJpeg();
    }

    public static boolean isJpeg(String mimeType) {
        return "image/jpeg".equals(mimeType) || "image/pjpeg".equals(mimeType);
    }

    public static ThumbnailFormat thumbnail(String mimeType) {
        return THUMBNAIL_FORMATS.getOrDefault(mimeType, null);
    }

    public static ThumbnailFormat downsize(String mimeType) {
        return LOSSY;
    }

    public static boolean isReasonableImage(String mimeType, Integer sizeX, Integer sizeY, Long fileSize) {
        if (!isSupportedImage(mimeType)) {
            return false;
        }
        if (sizeX != null && sizeY != null && (sizeX > 8192 || sizeY > 8192 || sizeX * sizeY > 25_000_000)) {
            return false;
        }
        if (fileSize == null) {
            return true;
        }
        return fileSize <= 5_242_880L;
    }

    public static boolean isReasonableImageForDownsize(String mimeType, Integer sizeX, Integer sizeY, Long fileSize) {
        if (!isSupportedImage(mimeType)) {
            return false;
        }
        if (sizeX != null && sizeY != null && (sizeX > 8192 || sizeY > 8192 || sizeX * sizeY > 40_000_000)) {
            return false;
        }
        if (fileSize == null) {
            return true;
        }
        if (MimeUtil.isJpeg(mimeType)) {
            return fileSize <= 20_971_520L;
        } else {
            return fileSize <= 5_242_880L;
        }
    }

}
