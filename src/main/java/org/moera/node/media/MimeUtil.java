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
    private static final Map<String, ThumbnailFormat> THUMBNAIL_FORMATS = new HashMap<>();

    static {
        var lossy = new ThumbnailFormat("image/jpeg", "JPEG");
        var lossless = new ThumbnailFormat("image/png", "PNG");
        THUMBNAIL_FORMATS.put("image/avif", lossy);
        THUMBNAIL_FORMATS.put("image/gif", lossless);
        THUMBNAIL_FORMATS.put("image/jp2", lossy);
        THUMBNAIL_FORMATS.put("image/jpeg", lossy);
        THUMBNAIL_FORMATS.put("image/pcx", lossless);
        THUMBNAIL_FORMATS.put("image/pjpeg", lossy);
        THUMBNAIL_FORMATS.put("image/png", lossless);
        THUMBNAIL_FORMATS.put("image/x-png", lossless);
        THUMBNAIL_FORMATS.put("image/svg+xml", lossless);
        THUMBNAIL_FORMATS.put("image/tiff", lossless);
        THUMBNAIL_FORMATS.put("image/vnd.microsoft.icon", lossless);
        THUMBNAIL_FORMATS.put("image/vnd.wap.wbmp", lossless);
        THUMBNAIL_FORMATS.put("image/webp", lossy);
        THUMBNAIL_FORMATS.put("image/x-ms-bmp", lossless);
        THUMBNAIL_FORMATS.put("image/x-portable-anymap", lossless);
        THUMBNAIL_FORMATS.put("image/x-portable-bitmap", lossless);
        THUMBNAIL_FORMATS.put("image/x-portable-graymap", lossless);
        THUMBNAIL_FORMATS.put("image/x-portable-pixmap", lossless);
        THUMBNAIL_FORMATS.put("image/x-xbitmap", lossless);
        THUMBNAIL_FORMATS.put("image/x-xpixmap", lossless);
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
        if (MimeUtil.isLossyImage(mimeType)) {
            return fileSize <= 5_242_880L;
        } else {
            return fileSize <= 3_145_728L;
        }
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
            return fileSize <= 3_145_728L;
        }
    }

}
