package org.moera.node.media;

import java.util.HashMap;
import java.util.Map;

public class MimeUtils {

    private static final Map<String, String> MIME_EXTENSIONS = new HashMap<>();

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
    }

    public static String extension(String mimeType) {
        return MIME_EXTENSIONS.getOrDefault(mimeType, "");
    }

}
