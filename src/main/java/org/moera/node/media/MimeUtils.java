package org.moera.node.media;

import java.util.HashMap;
import java.util.Map;

public class MimeUtils {

    public static class ThumbnailFormat {

        public String mimeType;
        public String format;

        public ThumbnailFormat(String mimeType, String format) {
            this.mimeType = mimeType;
            this.format = format;
        }

    }

    private static final Map<String, String> MIME_EXTENSIONS = new HashMap<>();
    private static final Map<String, ThumbnailFormat> THUMBNAIL_FORMATS = new HashMap<>();

    static {
        MIME_EXTENSIONS.put("image/avif", "avif");
        MIME_EXTENSIONS.put("image/gif", "gif");
        MIME_EXTENSIONS.put("image/jp2", "jp2");
        MIME_EXTENSIONS.put("image/jpeg", "jpg");
        MIME_EXTENSIONS.put("image/pcx", "pcx");
        MIME_EXTENSIONS.put("image/pjpeg", "jpg");
        MIME_EXTENSIONS.put("image/png", "png");
        MIME_EXTENSIONS.put("image/x-png", "png");
        MIME_EXTENSIONS.put("image/svg+xml", "svg");
        MIME_EXTENSIONS.put("image/tiff", "tiff");
        MIME_EXTENSIONS.put("image/vnd.djvu", "djvu");
        MIME_EXTENSIONS.put("image/vnd.microsoft.icon", "ico");
        MIME_EXTENSIONS.put("image/vnd.wap.wbmp", "wbmp");
        MIME_EXTENSIONS.put("image/webp", "webp");
        MIME_EXTENSIONS.put("image/x-ms-bmp", "bmp");
        MIME_EXTENSIONS.put("image/x-portable-anymap", "pnm");
        MIME_EXTENSIONS.put("image/x-portable-bitmap", "pbm");
        MIME_EXTENSIONS.put("image/x-portable-graymap", "pgm");
        MIME_EXTENSIONS.put("image/x-portable-pixmap", "ppm");
        MIME_EXTENSIONS.put("image/x-xbitmap", "xbm");
        MIME_EXTENSIONS.put("image/x-xpixmap", "xpm");
        MIME_EXTENSIONS.put("application/zip", "zip");

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
        return MIME_EXTENSIONS.getOrDefault(mimeType, "");
    }

    public static String fileName(String name, String mimeType) {
        return name + "." + extension(mimeType);
    }

    public static ThumbnailFormat thumbnail(String mimeType) {
        return THUMBNAIL_FORMATS.getOrDefault(mimeType, null);
    }

}
