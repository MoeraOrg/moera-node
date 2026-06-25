package org.moera.node.ui.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.MediaInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StoryInfo;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.global.RequestContext;
import org.moera.node.media.LocalRemoteMediaInfo;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MediaUtil;
import org.moera.node.media.MimeUtil;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

@HelperSource
public class GalleriesHelperSource {

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

    @Inject
    private ObjectMapper objectMapper;

    public CharSequence postingGalleries(
        PostingInfo posting,
        Collection<CommentInfo> comments,
        String canonicalUrl,
        String postingId,
        String commentId,
        String mediaId
    ) {
        Collection<MediaInfo> entries = new ArrayList<>();
        entries.add(posting);
        if (comments != null) {
            entries.addAll(comments);
        }
        return galleries(entries, canonicalUrl, postingId, commentId, mediaId);
    }

    public CharSequence feedGalleries(
        Collection<StoryInfo> stories,
        String canonicalUrl
    ) {
        return galleries(
            stories.stream().map(StoryInfo::getPosting).collect(Collectors.toList()), canonicalUrl, null, null, null
        );
    }

    private CharSequence galleries(
        Collection<MediaInfo> entries,
        String canonicalUrl,
        String galleryPostingId,
        String galleryCommentId,
        String galleryMediaId
    ) {
        Map<String, Map<String, String>[]> entryMap = new HashMap<>();
        for (MediaInfo entry : entries) {
            var props = entry.getMedia()
                .stream()
                .map(this::galleryItemProps)
                .filter(Objects::nonNull)
                .toArray(Map[]::new);
            entryMap.put(entry.getId(), props);
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<script>");
        buf.append("window.galleries = ");
        buf.append(objectMapper.writeValueAsString(entryMap));
        buf.append(";");
        buf.append("window.canonicalUrl = \"");
        buf.append(canonicalUrl);
        buf.append("\";");
        buf.append("window.galleryPostingId = ");
        if (galleryPostingId != null) {
            buf.append("\"");
            buf.append(galleryPostingId);
            buf.append("\";");
        } else {
            buf.append("null;");
        }
        buf.append("window.galleryCommentId = ");
        if (galleryCommentId != null) {
            buf.append("\"");
            buf.append(galleryCommentId);
            buf.append("\";");
        } else {
            buf.append("null;");
        }
        buf.append("window.galleryMediaId = ");
        if (galleryMediaId != null) {
            buf.append("\"");
            buf.append(galleryMediaId);
            buf.append("\";");
        } else {
            buf.append("null;");
        }
        buf.append("</script>");
        return new SafeString(buf);
    }

    private Map<String, String> galleryItemProps(MediaAttachment attachment) {
        String captionUrl = !ObjectUtils.isEmpty(attachment.getPostingId())
            ? "/moera/media/private/caption/" + attachment.getPostingId()
            : "";

        var media = attachment.getMedia();
        if (media != null) {
            return Map.of(
                "id", media.getId(),
                "src", "/moera/media/" + media.getPath(),
                "thumb", "/moera/media/" + media.getPath() + "?width=150",
                "subHtmlUrl", captionUrl
            );
        }

        var remoteMedia = attachment.getRemoteMedia();
        if (remoteMedia == null || remoteMedia.getNodeName() == null || remoteMedia.getMediaId() == null) {
            return null;
        }

        String nodeName = remoteMedia.getNodeName();
        var nameDetails = namingCache.getFast(nodeName);
        String prefix = nameDetails.getNodeUri() != null
            ? nameDetails.getNodeUri() + "/media/"
            : "/moera/remote-media/" + nodeName + "/";
        String grant = new MediaGrantGenerator(requestContext.getOptions())
            .generatePublicRemote(remoteMedia.getMediaId(), false, null);
        return Map.of(
            "id", remoteMedia.getMediaId(),
            "src", prefix + MediaUtil.privatePath(remoteMedia, null, grant),
            "thumb", prefix + MediaUtil.privatePath(remoteMedia, 150, grant),
            "subHtmlUrl", captionUrl
        );
    }

    private CharSequence entryImage(String postingId, String commentId, LocalRemoteMediaInfo mediaFile) {
        return entryImage(postingId, commentId, mediaFile, null, null);
    }

    private CharSequence entryImage(String postingId, String commentId, LocalRemoteMediaInfo mediaFile, String flex) {
        return entryImage(postingId, commentId, mediaFile, flex, null);
    }

    private CharSequence entryImage(
        String postingId, String commentId, LocalRemoteMediaInfo mediaFile, String flex, Integer count
    ) {
        String style = null;
        if (flex != null || count != null) {
            Integer width = mediaFile.width();
            Integer height = mediaFile.height();
            if (width != null && height != null && width > 0 && height > 0) {
                if ("row".equals(flex)) {
                    style = "flex: %f".formatted(((float) width) / height);
                } else if ("column".equals(flex)) {
                    style = "flex: %f".formatted(((float) height) / width);
                }
            } else {
                style = "flex: 1";
            }
        }

        String mediaId = mediaFile.mediaId();
        mediaId = mediaId != null ? mediaId : "";
        String klass = count != null && count > 0 ? "entry-image counted" : "entry-image";
        String href = commentId != null
            ? "/post/%s?comment=%s&media=%s"
            .formatted(Util.ue(postingId), Util.ue(commentId), Util.ue(mediaId)
            )
            : "/post/%s?media=%s".formatted(Util.ue(postingId), Util.ue(mediaId));

        StringBuilder buf = new StringBuilder();

        buf.append("<a");
        HelperUtil.appendAttr(buf, "href", href);
        HelperUtil.appendAttr(buf, "class", klass);
        HelperUtil.appendAttr(buf, "data-id", mediaFile.mediaId());
        HelperUtil.appendAttr(buf, "style", style);
        buf.append('>');

        if (count != null && count > 0) {
            buf.append("<div class=\"count\">+%d</div>".formatted(count));
        }

        MediaFilePreviewInfo preview = MediaFilePreviewInfoUtil.findLargerPreview(mediaFile.previews(), 900);

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
            HelperUtil.appendAttr(buf, "src", mediaFile.path(namingCache, requestContext.getOptions()));
            HelperUtil.appendAttr(buf, "width", mediaFile.width());
            HelperUtil.appendAttr(buf, "height", mediaFile.height());
        }
        if (mediaFile.local() != null) {
            HelperUtil.appendAttr(buf, "srcset", MediaUtil.mediaSources(mediaFile.local()));
            HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile.local()));
        }
        String alt = mediaFile.textContent();
        alt = alt != null ? alt : "";
        HelperUtil.appendAttr(buf, "alt", alt);
        buf.append('>');

        buf.append("</a>");

        return new SafeString(buf);
    }

    private int singleImageHeight(LocalRemoteMediaInfo image) {
        int maxWidth = requestContext.getOptions().getInt("feed.width") - 25;
        Integer width = image.width();
        Integer height = image.height();
        if (width != null && height != null && width > 0 && height > 0) {
            return width <= maxWidth ? height : height * maxWidth / width;
        }
        return 0;
    }

    private String majorOrientation(List<LocalRemoteMediaInfo> images) {
        int balance = 0;
        for (int i = 0; i < 6 && i < images.size(); i++) {
            Integer width = images.get(i).width();
            Integer height = images.get(i).height();
            if (width != null && height != null) {
                balance += height < width ? 1 : -1;
            }
        }
        return balance >= 0 ? "vertical" : "horizontal";
    }

    public CharSequence entryGallery(String postingId, String commentId, List<MediaAttachment> media) {
        if (ObjectUtils.isEmpty(media)) {
            return null;
        }

        List<LocalRemoteMediaInfo> images = media.stream()
            .filter(ma -> !ma.isEmbedded())
            .map(LocalRemoteMediaInfo::new)
            .filter(m -> !m.attachment())
            .collect(Collectors.toList());
        if (images.isEmpty()) {
            return null;
        }

        String orientation = majorOrientation(images);

        StringBuilder buf = new StringBuilder();

        switch (images.size()) {
            case 1:
                buf.append("<div");
                HelperUtil.appendAttr(buf, "class", "gallery single %s".formatted(orientation));
                HelperUtil.appendAttr(
                    buf,
                    "style",
                    "--image-height: %dpx".formatted(singleImageHeight(images.get(0)))
                );
                buf.append('>');
                buf.append(entryImage(postingId, commentId, images.get(0)));
                buf.append("</div>");
                break;

            case 2:
                buf.append("<div class=\"gallery %s\">".formatted(orientation));
                buf.append(entryImage(postingId, commentId, images.get(0), "row"));
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append("</div>");
                break;

            case 3:
                buf.append("<div class=\"gallery %s\">".formatted(orientation));
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(0), null));
                buf.append("</div>");
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append(entryImage(postingId, commentId, images.get(2), "row"));
                buf.append("</div>");
                buf.append("</div>");
                break;

            case 4:
                buf.append("<div class=\"gallery %s\">".formatted(orientation));
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(0), "row"));
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append("</div>");
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(2), "row"));
                buf.append(entryImage(postingId, commentId, images.get(3), "row"));
                buf.append("</div>");
                buf.append("</div>");
                break;

            case 5:
                buf.append("<div class=\"gallery %s\">".formatted(orientation));
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(0), "row"));
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append("</div>");
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(2), "row"));
                buf.append(entryImage(postingId, commentId, images.get(3), "row"));
                buf.append(entryImage(postingId, commentId, images.get(4), "row"));
                buf.append("</div>");
                buf.append("</div>");
                break;

            default:
                buf.append("<div class=\"gallery %s\">".formatted(orientation));
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(0), "row"));
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append("</div>");
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(2), "row"));
                buf.append(entryImage(postingId, commentId, images.get(3), "row"));
                buf.append("</div>");
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, commentId, images.get(4), "row"));
                buf.append(entryImage(postingId, commentId, images.get(5), "row", images.size() - 6));
                buf.append("</div>");
                buf.append("</div>");
        }

        return new SafeString(buf);
    }

    public CharSequence entryAttachments(String postingId, String commentId, List<MediaAttachment> media) {
        if (ObjectUtils.isEmpty(media)) {
            return null;
        }

        List<LocalRemoteMediaInfo> files = media.stream()
            .map(LocalRemoteMediaInfo::new)
            .filter(LocalRemoteMediaInfo::attachment)
            .filter(m -> m.local() == null || !Boolean.TRUE.equals(m.local().getMalware()))
            .toList();
        if (files.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();

        buf.append("<div>");
        for (LocalRemoteMediaInfo file : files) {
            buf.append("<div class=\"attached-file\">");
            buf.append("<i class=\"fas fa-download download-icon\"></i>");
            String fileLocation = file.path(namingCache, requestContext.getOptions(), true);
            buf.append(
                "<a class=\"file-name\" download href=\"%s\" title=\"Download\">".formatted(Util.he(fileLocation))
            );
            String fileName = MimeUtil.fileName(
                !ObjectUtils.isEmpty(file.title()) ? file.title() : file.hash(),
                file.mimeType()
            );
            buf.append(Util.he(fileName));
            buf.append("</a>");
            buf.append("</div>");
        }
        buf.append("</div>");

        return new SafeString(buf);
    }

}
