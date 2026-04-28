package org.moera.node.ui.helper;

import static org.moera.node.util.Util.ellipsize;

import java.util.List;

import com.github.jknack.handlebars.Handlebars;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.media.MediaUtil;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

@HelperSource
public class LinkPreviewHelper {

    public CharSequence linkPreview(
        String siteName,
        String url,
        String title,
        String description,
        String imageHash,
        List<MediaAttachment> media,
        boolean small,
        Object noFollow
    ) {
        if (ObjectUtils.isEmpty(url)) {
            return null;
        }

        String host;
        try {
            host = UriComponentsBuilder.fromUriString(url).build().getHost();
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
            mediaFile = media.stream()
                .map(MediaAttachment::getMedia)
                .filter(mf -> mf != null && mf.getHash().equals(imageHash))
                .findFirst()
                .orElse(null);
            large = !small && mediaFile != null && mediaFile.getWidth() > 450;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<a");
        HelperUtil.appendAttr(buf, "href", url);
        HelperUtil.appendAttr(
            buf, "class", "link-preview" + (large ? " large" : "") + (small ? " small" : "")
        );
        if (HelperUtil.boolArg(noFollow)) {
            HelperUtil.appendAttr(buf, "rel", "nofollow");
        }
        buf.append('>');
        if (mediaFile != null) {
            MediaFilePreviewInfo preview = MediaFilePreviewInfoUtil.findLargerPreview(mediaFile.getPreviews(), 800);

            buf.append("<img");
            if (preview != null) {
                HelperUtil.appendAttr(
                    buf,
                    "src",
                    "/moera/media/" + (preview.getDirectPath() != null ? preview.getDirectPath() : preview.getPath())
                );
                HelperUtil.appendAttr(buf, "width", preview.getWidth());
                HelperUtil.appendAttr(buf, "height", preview.getHeight());
            } else {
                HelperUtil.appendAttr(
                    buf,
                    "src",
                    "/moera/media/"
                        + (mediaFile.getDirectPath() != null ? mediaFile.getDirectPath() : mediaFile.getPath())
                );
                HelperUtil.appendAttr(buf, "width", mediaFile.getWidth());
                HelperUtil.appendAttr(buf, "height", mediaFile.getHeight());
            }
            HelperUtil.appendAttr(buf, "srcset", MediaUtil.mediaSources(mediaFile));
            HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile));
            if (mediaFile.getHeight() > mediaFile.getWidth()) {
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
