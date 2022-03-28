package org.moera.node.ui.helper;

import static org.moera.node.util.Util.ellipsize;

import java.util.Arrays;

import com.github.jknack.handlebars.Handlebars;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.MediaFilePreviewInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.MediaUtil;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

@HelperSource
public class LinkPreviewHelper {

    public CharSequence linkPreview(String siteName, String url, String title, String description, String imageHash,
                                    MediaAttachment[] media, boolean small) {
        if (ObjectUtils.isEmpty(url)) {
            return null;
        }

        String host;
        try {
            host = UriComponentsBuilder.fromHttpUrl(url).build().getHost();
            if (ObjectUtils.isEmpty(host)) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            // illegal URL
            return null;
        }

        boolean large = false;
        PrivateMediaFileInfo mediaFile = null;
        if (imageHash != null && media != null) {
            mediaFile = Arrays.stream(media)
                    .map(MediaAttachment::getMedia)
                    .filter(mf -> mf != null && mf.getHash().equals(imageHash))
                    .findFirst()
                    .orElse(null);
            large = !small && mediaFile != null && mediaFile.getWidth() > 450;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<a");
        HelperUtil.appendAttr(buf, "href", url);
        HelperUtil.appendAttr(buf, "class",
                "link-preview" + (large ? " large" : "") + (small ? " small" : ""));
        buf.append('>');
        if (mediaFile != null) {
            String mediaLocation = "/moera/media/" + mediaFile.getPath();

            MediaFilePreviewInfo preview = mediaFile.findLargerPreview(800);
            int imageWidth = preview != null ? preview.getWidth() : mediaFile.getWidth();
            int imageHeight = preview != null ? preview.getHeight() : mediaFile.getHeight();

            buf.append("<img");
            HelperUtil.appendAttr(buf, "src", MediaUtil.mediaPreview(mediaLocation, 800));
            HelperUtil.appendAttr(buf, "srcset",
                    MediaUtil.mediaSourcesInfo(mediaLocation, Arrays.asList(mediaFile.getPreviews())));
            HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile));
            HelperUtil.appendAttr(buf, "width", imageWidth);
            HelperUtil.appendAttr(buf, "height", imageHeight);
            if (imageHeight > imageWidth) {
                HelperUtil.appendAttr(buf, "class", "vertical");
            }
            buf.append('>');
        }
        buf.append("<div class=\"details\">");
        if (!ObjectUtils.isEmpty(title)) {
            buf.append("<div class=\"title\">");
            buf.append(ellipsize(title, small ? 35 : 75));
            buf.append("</div>");
        }
        if (!ObjectUtils.isEmpty(description)) {
            buf.append("<div class=\"description\">");
            buf.append(ellipsize(description, small ? 70 : 120));
            buf.append("</div>");
        }
        buf.append("<div class=\"site\">");
        if (!ObjectUtils.isEmpty(siteName)) {
            buf.append(ellipsize(siteName, 40));
            buf.append("<span class=\"bullet\">&bull;</span>");
        }
        buf.append(host.toUpperCase());
        buf.append("</div>");
        buf.append("</div>");
        buf.append("</a>");

        return new Handlebars.SafeString(buf);
    }

}
