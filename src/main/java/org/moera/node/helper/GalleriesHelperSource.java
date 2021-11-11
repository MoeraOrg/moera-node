package org.moera.node.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.MediaFilePreviewInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.util.MediaUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

@HelperSource
public class GalleriesHelperSource {

    @Inject
    private ObjectMapper objectMapper;

    public CharSequence galleries(Object objects, String canonicalUrl, String entryId,
                                  String mediaId) throws JsonProcessingException {
        Collection<?> entries;
        if (objects instanceof Collection) {
            entries = (Collection<?>) objects;
        } else {
            entries = List.of(objects);
        }

        Map<String, Map<String, String>[]> entryMap = new HashMap<>();
        for (Object entry : entries) {
            PostingInfo posting = null;
            if (entry instanceof PostingInfo) {
                posting = (PostingInfo) entry;
            } else if (entry instanceof StoryInfo) {
                posting = ((StoryInfo) entry).getPosting();
            }
            if (posting == null) {
                continue;
            }
            var props = Arrays.stream(posting.getMedia())
                    .map(MediaAttachment::getMedia)
                    .map(mfo -> Map.of(
                            "id", mfo.getId(),
                            "src", "/moera/media/" + mfo.getPath(),
                            "thumb", "/moera/media/" + mfo.getPath() + "?width=150"))
                    .toArray(Map[]::new);
            entryMap.put(posting.getId(), props);
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<script>");
        buf.append("window.galleries = ");
        buf.append(objectMapper.writeValueAsString(entryMap));
        buf.append(";");
        buf.append("window.canonicalUrl = \"");
        buf.append(canonicalUrl);
        buf.append("\";");
        buf.append("window.galleryEntryId = ");
        if (entryId != null) {
            buf.append("\"");
            buf.append(entryId);
            buf.append("\";");
        } else {
            buf.append("null;");
        }
        buf.append("window.galleryMediaId = ");
        if (mediaId != null) {
            buf.append("\"");
            buf.append(mediaId);
            buf.append("\";");
        } else {
            buf.append("null;");
        }
        buf.append("</script>");
        return new SafeString(buf);
    }

    private CharSequence entryImage(String postingId, PrivateMediaFileInfo mediaFile) {
        return entryImage(postingId, mediaFile, null, null);
    }

    private CharSequence entryImage(String postingId, PrivateMediaFileInfo mediaFile, String flex) {
        return entryImage(postingId, mediaFile, flex, null);
    }

    private CharSequence entryImage(String postingId, PrivateMediaFileInfo mediaFile, String flex, Integer count) {
        MediaFilePreviewInfo preview = mediaFile.findLargerPreview(900);
        int imageWidth = preview != null ? preview.getWidth() : mediaFile.getWidth();
        int imageHeight = preview != null ? preview.getHeight() : mediaFile.getHeight();

        String style = null;
        if (flex != null || count != null) {
            List<String> styles = new ArrayList<>();
            if ("row".equals(flex)) {
                styles.add(String.format("flex: %f", ((float) imageWidth) / imageHeight));
            } else if ("column".equals(flex)) {
                styles.add(String.format("flex: %f", ((float) imageHeight) / imageWidth));
            }
            if (count != null && count > 0) {
                styles.add("position: relative");
            }
            style = String.join(";", styles);
        }

        StringBuilder buf = new StringBuilder();

        buf.append("<a");
        HelperUtil.appendAttr(buf, "href", String.format("/post/%s?media=%s",
                Util.ue(postingId), Util.ue(mediaFile.getId())));
        HelperUtil.appendAttr(buf, "class", "entry-image");
        HelperUtil.appendAttr(buf, "data-id", mediaFile.getId());
        HelperUtil.appendAttr(buf, "style", style);
        buf.append('>');

        if (count != null && count > 0) {
            buf.append(String.format("<div class=\"count\">+%d</div>", count));
        }

        String mediaLocation = "/moera/media/" + mediaFile.getPath();
        buf.append("<img");
        HelperUtil.appendAttr(buf, "src", MediaUtil.mediaPreview(mediaLocation, 900));
        HelperUtil.appendAttr(buf, "srcset",
                MediaUtil.mediaSourcesInfo(mediaLocation, Arrays.asList(mediaFile.getPreviews())));
        HelperUtil.appendAttr(buf, "sizes", MediaUtil.mediaSizes(mediaFile));
        HelperUtil.appendAttr(buf, "width", imageWidth);
        HelperUtil.appendAttr(buf, "height", imageHeight);
        buf.append('>');

        buf.append("</a>");

        return new SafeString(buf);
    }

    public CharSequence postingGallery(String postingId, MediaAttachment[] media) {
        if (ObjectUtils.isEmpty(media)) {
            return null;
        }

        List<PrivateMediaFileInfo> images = Arrays.stream(media)
                .filter(ma -> !ma.isEmbedded())
                .map(MediaAttachment::getMedia)
                .collect(Collectors.toList());
        if (images.size() == 0) {
            return null;
        }

        String orientation = images.get(0).getHeight() < images.get(0).getWidth() ? "vertical" : "horizontal";

        StringBuilder buf = new StringBuilder();

        if (images.size() == 1) {
            buf.append("<div");
            HelperUtil.appendAttr(buf, "class", String.format("gallery single %s", orientation));
            HelperUtil.appendAttr(buf, "style",
                    String.format("--image-height: %dpx", images.get(0).getHeight()));
            buf.append('>');
            buf.append(entryImage(postingId, images.get(0)));
            buf.append("</div>");
        } else if (images.size() == 2) {
            buf.append(String.format("<div class=\"gallery %s\">", orientation));
            buf.append(entryImage(postingId, images.get(0), "row"));
            buf.append(entryImage(postingId, images.get(1), "row"));
            buf.append("</div>");
        } else {
            int base = images.size() > 6 ? 0 : images.size() % 2;

            buf.append(String.format("<div class=\"gallery %s\">", orientation));
            buf.append("<div class=\"gallery-row\">");
            buf.append(entryImage(postingId, images.get(0), base == 0 ? "row" : null));
            if (base == 0) {
                buf.append(entryImage(postingId, images.get(1), "row"));
            }
            buf.append("</div>");
            if (images.size() > 2) {
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, images.get(2 - base), "row"));
                buf.append(entryImage(postingId, images.get(3 - base), "row"));
                buf.append("</div>");
            }
            if (images.size() > 4) {
                buf.append("<div class=\"gallery-row\">");
                buf.append(entryImage(postingId, images.get(4 - base), "row"));
                buf.append(entryImage(postingId, images.get(5 - base), "row", images.size() - 6));
                buf.append("</div>");
            }
            buf.append("</div>");
        }

        return new SafeString(buf);
    }

}
