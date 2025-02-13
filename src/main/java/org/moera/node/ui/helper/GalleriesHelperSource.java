package org.moera.node.ui.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.model.MediaInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.util.MediaUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

@HelperSource
public class GalleriesHelperSource {

    @Inject
    private RequestContext requestContext;

    @Inject
    private ObjectMapper objectMapper;

    public CharSequence postingGalleries(PostingInfo posting, Collection<CommentInfo> comments, String canonicalUrl,
                                         String postingId, String commentId,
                                         String mediaId) throws JsonProcessingException {
        Collection<MediaInfo> entries = new ArrayList<>();
        entries.add(posting);
        if (comments != null) {
            entries.addAll(comments);
        }
        return galleries(entries, canonicalUrl, postingId, commentId, mediaId);
    }

    public CharSequence feedGalleries(Collection<StoryInfo> stories,
                                      String canonicalUrl) throws JsonProcessingException {
        return galleries(stories.stream().map(StoryInfo::getPosting).collect(Collectors.toList()),
                canonicalUrl, null, null, null);
    }

    private CharSequence galleries(Collection<MediaInfo> entries, String canonicalUrl, String galleryPostingId,
                                   String galleryCommentId, String galleryMediaId) throws JsonProcessingException {
        Map<String, Map<String, String>[]> entryMap = new HashMap<>();
        for (MediaInfo entry : entries) {
            var props = Arrays.stream(entry.getMedia())
                    .map(MediaAttachment::getMedia)
                    .map(mfo -> Map.of(
                        "id", mfo.getId(),
                        "src", "/moera/media/" + mfo.getPath(),
                        "thumb", "/moera/media/" + mfo.getPath() + "?width=150",
                        "subHtmlUrl", "/moera/media/private/" + mfo.getId() + "/caption"
                    ))
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

    private CharSequence entryImage(String postingId, String commentId, PrivateMediaFileInfo mediaFile) {
        return entryImage(postingId, commentId, mediaFile, null, null);
    }

    private CharSequence entryImage(String postingId, String commentId, PrivateMediaFileInfo mediaFile, String flex) {
        return entryImage(postingId, commentId, mediaFile, flex, null);
    }

    private CharSequence entryImage(String postingId, String commentId, PrivateMediaFileInfo mediaFile, String flex,
                                    Integer count) {
        MediaFilePreviewInfo preview = MediaFilePreviewInfoUtil.findLargerPreview(mediaFile.getPreviews(), 900);
        int imageWidth = preview != null ? preview.getWidth() : mediaFile.getWidth();
        int imageHeight = preview != null ? preview.getHeight() : mediaFile.getHeight();

        String style = null;
        if (flex != null || count != null) {
            if ("row".equals(flex)) {
                style = String.format("flex: %f", ((float) imageWidth) / imageHeight);
            } else if ("column".equals(flex)) {
                style = String.format("flex: %f", ((float) imageHeight) / imageWidth);
            }
        }

        String klass = count != null && count > 0 ? "entry-image counted" : "entry-image";
        String href = commentId != null
                ? String.format("/post/%s?comment=%s&media=%s", Util.ue(postingId), Util.ue(commentId),
                                Util.ue(mediaFile.getId()))
                : String.format("/post/%s?media=%s", Util.ue(postingId), Util.ue(mediaFile.getId()));

        StringBuilder buf = new StringBuilder();

        buf.append("<a");
        HelperUtil.appendAttr(buf, "href", href);
        HelperUtil.appendAttr(buf, "class", klass);
        HelperUtil.appendAttr(buf, "data-id", mediaFile.getId());
        HelperUtil.appendAttr(buf, "style", style);
        buf.append('>');

        if (count != null && count > 0) {
            buf.append(String.format("<div class=\"count\">+%d</div>", count));
        }

        boolean directServing = mediaFile.getDirectPath() != null;
        String mediaLocation = "/moera/media/" + (directServing ? mediaFile.getDirectPath() : mediaFile.getPath());
        buf.append("<img");
        HelperUtil.appendAttr(buf, "src",
                directServing ? mediaLocation : MediaUtil.mediaPreview(mediaLocation, 900));
        HelperUtil.appendAttr(buf, "srcset", MediaUtil.mediaSourcesInfo(mediaLocation, mediaFile.getPreviews()));
        HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile));
        HelperUtil.appendAttr(buf, "width", imageWidth);
        HelperUtil.appendAttr(buf, "height", imageHeight);
        buf.append('>');

        buf.append("</a>");

        return new SafeString(buf);
    }

    private int singleImageHeight(PrivateMediaFileInfo image) {
        int maxWidth = requestContext.getOptions().getInt("feed.width") - 25;
        return image.getWidth() <= maxWidth ? image.getHeight() : image.getHeight() * maxWidth / image.getWidth();
    }

    private String majorOrientation(List<PrivateMediaFileInfo> images) {
        int balance = 0;
        for (int i = 0; i < 6 && i < images.size(); i++) {
            balance += images.get(i).getHeight() < images.get(i).getWidth() ? 1 : -1;
        }
        return balance >= 0 ? "vertical" : "horizontal";
    }

    public CharSequence entryGallery(String postingId, String commentId, MediaAttachment[] media) {
        if (ObjectUtils.isEmpty(media)) {
            return null;
        }

        List<PrivateMediaFileInfo> images = Arrays.stream(media)
                .filter(ma -> !Boolean.TRUE.equals(ma.getEmbedded()))
                .map(MediaAttachment::getMedia)
                .collect(Collectors.toList());
        if (images.isEmpty()) {
            return null;
        }

        String orientation = majorOrientation(images);

        StringBuilder buf = new StringBuilder();

        switch (images.size()) {
            case 1:
                buf.append("<div");
                HelperUtil.appendAttr(buf, "class", String.format("gallery single %s", orientation));
                HelperUtil.appendAttr(buf, "style",
                        String.format("--image-height: %dpx", singleImageHeight(images.get(0))));
                buf.append('>');
                buf.append(entryImage(postingId, commentId, images.get(0)));
                buf.append("</div>");
                break;

            case 2:
                buf.append(String.format("<div class=\"gallery %s\">", orientation));
                buf.append(entryImage(postingId, commentId, images.get(0), "row"));
                buf.append(entryImage(postingId, commentId, images.get(1), "row"));
                buf.append("</div>");
                break;

            case 3:
                buf.append(String.format("<div class=\"gallery %s\">", orientation));
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
                buf.append(String.format("<div class=\"gallery %s\">", orientation));
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
                buf.append(String.format("<div class=\"gallery %s\">", orientation));
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
                buf.append(String.format("<div class=\"gallery %s\">", orientation));
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

}
