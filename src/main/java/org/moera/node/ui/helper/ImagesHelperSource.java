package org.moera.node.ui.helper;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

@HelperSource
public class ImagesHelperSource {

    private static final Logger log = LoggerFactory.getLogger(ImagesHelperSource.class);

    @Inject
    private ApplicationContext applicationContext;

    private final Map<String, Dimension> imageSizeCache = new HashMap<>();

    public CharSequence image(CharSequence src, Options options) {
        CharSequence alt = options.hash("alt");
        CharSequence title = options.hash("title");
        CharSequence klass = options.hash("class");
        CharSequence id = options.hash("id");
        Object dataId = options.hash("dataId");
        Object dataValue = options.hash("dataValue");
        CharSequence style = options.hash("style");
        boolean button = HelperUtil.boolArg(options.hash("button", false));

        StringBuilder buf = new StringBuilder();
        buf.append(!button ? "<img" : "<input type=\"image\"");
        HelperUtil.appendAttr(buf, "src", src);
        Dimension imageSize = getImageSize(src.toString());
        if (imageSize != null) {
            HelperUtil.appendAttr(buf, "width", imageSize.width);
            HelperUtil.appendAttr(buf, "height", imageSize.height);
        }
        HelperUtil.appendAttr(buf, "alt", alt);
        HelperUtil.appendAttr(buf, "title", title);
        HelperUtil.appendAttr(buf, "class", klass);
        HelperUtil.appendAttr(buf, "id", id);
        HelperUtil.appendAttr(buf, "data-id", dataId);
        HelperUtil.appendAttr(buf, "data-value", dataValue);
        HelperUtil.appendAttr(buf, "style", style);
        buf.append('>');
        return new SafeString(buf);
    }

    CharSequence image(CharSequence src) {
        return image(src, null, null, null, null, null, null, null);
    }

    CharSequence image(CharSequence src, CharSequence alt, CharSequence title) {
        return image(src, alt, title, null, null, null, null, null);
    }

    CharSequence image(CharSequence src, CharSequence alt, CharSequence title, CharSequence style,
                       CharSequence klass) {
        return image(src, alt, title, style, klass, null, null, null);
    }

    CharSequence image(CharSequence src, CharSequence alt, CharSequence title, CharSequence style,
                       CharSequence klass, Object dataId, Object dataValue, CharSequence id) {
        StringBuilder buf = new StringBuilder();
        buf.append("<img");
        HelperUtil.appendAttr(buf, "src", src);
        Dimension imageSize = getImageSize(src.toString());
        if (imageSize != null) {
            HelperUtil.appendAttr(buf, "width", imageSize.width);
            HelperUtil.appendAttr(buf, "height", imageSize.height);
        }
        HelperUtil.appendAttr(buf, "alt", alt);
        HelperUtil.appendAttr(buf, "title", title);
        HelperUtil.appendAttr(buf, "class", klass);
        HelperUtil.appendAttr(buf, "data-id", dataId);
        HelperUtil.appendAttr(buf, "data-value", dataValue);
        HelperUtil.appendAttr(buf, "style", style);
        HelperUtil.appendAttr(buf, "id", id);
        buf.append('>');
        return new SafeString(buf);
    }

    private Dimension getImageSize(String path) {
        Dimension imageSize = imageSizeCache.get(path);
        if (imageSize != null) {
            return imageSize;
        }

        try {
            BufferedImage image =
                    ImageIO.read(applicationContext.getResource("classpath:static" + path).getInputStream());
            imageSize = new Dimension(image.getWidth(), image.getHeight());
            imageSizeCache.put(path, imageSize);
            return imageSize;
        } catch (IOException e) {
            log.error("Error determining size of the image '{}': {}", path, e.getMessage());
            return null;
        }
    }

}
