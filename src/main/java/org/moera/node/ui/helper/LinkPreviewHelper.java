package org.moera.node.ui.helper;

import static org.moera.node.util.Util.ellipsize;

import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.Handlebars;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.global.RequestContext;
import org.moera.node.media.LocalRemoteMediaInfo;
import org.moera.node.media.MediaUtil;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

@HelperSource
public class LinkPreviewHelper {

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

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
        LocalRemoteMediaInfo mediaFile = null;
        if (imageHash != null && media != null) {
            mediaFile = media.stream()
                .map(LocalRemoteMediaInfo::new)
                .filter(mf -> Objects.equals(mf.hash(), imageHash))
                .findFirst()
                .orElse(null);
            Integer width = mediaFile != null ? mediaFile.width() : null;
            large = !small && width != null && width > 450;
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
            MediaFilePreviewInfo preview = MediaFilePreviewInfoUtil.findLargerPreview(mediaFile.previews(), 800);

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
                    mediaFile.path(namingCache, requestContext.getOptions())
                );
                HelperUtil.appendAttr(buf, "width", mediaFile.width());
                HelperUtil.appendAttr(buf, "height", mediaFile.height());
            }
            if (mediaFile.local() != null) {
                HelperUtil.appendAttr(buf, "srcset", MediaUtil.mediaSources(mediaFile.local()));
                HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile.local()));
            }
            Integer width = mediaFile.width();
            Integer height = mediaFile.height();
            if (width != null && height != null && height > width) {
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
