package org.moera.node.media;

import java.util.HashMap;
import java.util.Map;

import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

public class MimeUtil {

    public static class ThumbnailFormat {

        public String mimeType;
        public String format;

        public ThumbnailFormat(String mimeType, String format) {
            this.mimeType = mimeType;
            this.format = format;
        }

    }

    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();
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
        try {
            var extension = MIME_TYPES.forName(mimeType).getExtension();
            return extension.startsWith(".") ? extension.substring(1) : extension;
        } catch (MimeTypeException e) {
            return "";
        }
    }

    public static String fileName(String name, String mimeType) {
        return name + "." + extension(mimeType);
    }

    public static boolean isSupportedImage(String mimeType) {
        return THUMBNAIL_FORMATS.containsKey(mimeType);
    }

    public static ThumbnailFormat thumbnail(String mimeType) {
        return THUMBNAIL_FORMATS.getOrDefault(mimeType, null);
    }

}
