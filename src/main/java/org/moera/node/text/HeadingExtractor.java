package org.moera.node.text;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class HeadingExtractor {

    public static final String EMOJI_PICTURE = Character.toString(0x1f5bc);
    public static final String EMOJI_SCROLL = Character.toString(0x1f4dc);
    public static final String EMOJI_CHAIN = Character.toString(0x1f517);
    public static final String EMOJI_MOVIE = Character.toString(0x1f4fd);

    private static final int HEADING_LENGTH = 80;
    private static final int DESCRIPTION_LENGTH = 200;

    private static final Pattern URL = Pattern.compile(
        "https?://[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9]{1,6}"
            + "\\b(?:[-a-zA-Z0-9()!@:%_+,.;~#?&/=]*[-a-zA-Z0-9@:%_+~#&/=])?",
        Pattern.CASE_INSENSITIVE
    );

    public static String extractHeading(Body body, List<MediaFileOwner> media, boolean collapseQuotations) {
        if (!ObjectUtils.isEmpty(body.getSubject())) {
            return Util.ellipsize(body.getSubject(), HEADING_LENGTH);
        }
        String text = URL.matcher(body.getAllText()).replaceAll(EMOJI_CHAIN);
        String heading = extract(text, HEADING_LENGTH, collapseQuotations);
        if (heading.length() < HEADING_LENGTH) {
            String galleryText = extractGalleryTexts(body, media, HEADING_LENGTH - heading.length() - 1);
            if (!ObjectUtils.isEmpty(galleryText)) {
                heading += ' ' + galleryText;
            }
        }
        return heading;
    }

    public static String extractDescription(
        Body body, List<MediaFileOwner> media, boolean collapseQuotations, String heading
    ) {
        String text = body.getAllText();
        if (ObjectUtils.isEmpty(text)) {
            return "";
        }
        text = URL.matcher(text).replaceAll(EMOJI_CHAIN);
        int beginningLength = getDescriptionBeginningLength(body, heading);
        int descriptionLength = DESCRIPTION_LENGTH + beginningLength;
        String description = extract(text, descriptionLength, collapseQuotations);
        if (description.length() < descriptionLength) {
            String galleryText = extractGalleryTexts(body, media, descriptionLength - description.length() - 1);
            if (!ObjectUtils.isEmpty(galleryText)) {
                description += ' ' + galleryText;
            }
        }
        description = beginningLength < description.length() - 1 ? description.substring(beginningLength) : "";
        if (beginningLength != 0 && !ObjectUtils.isEmpty(description)) {
            description = '\u2026' + description;
        }
        return description;
    }

    private static int getDescriptionBeginningLength(Body body, String heading) {
        if (!ObjectUtils.isEmpty(body.getSubject()) || ObjectUtils.isEmpty(heading)) {
            return 0;
        }
        return heading.endsWith("\u2026") ? heading.length() - 1 : heading.length();
    }

    private static String extract(String html, int len, boolean collapseQuotations) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor(len, collapseQuotations);
        document.filter(extractor);
        return extractor.getResult();
    }

    private static class Extractor implements NodeFilter {

        private final StringBuilder result = new StringBuilder();
        private int ignoreContent = 0;
        private final int len;
        private final boolean collapseQuotations;

        Extractor(int len, boolean collapseQuotations) {
            this.len = len;
            this.collapseQuotations = collapseQuotations;
        }

        String getResult() {
            return result.toString();
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (ignoreContent <= 0) {
                String text = null;
                if (node instanceof TextNode textNode) {
                    text = clear(textNode.text().trim());
                } else if (node instanceof Element element) {
                    if (element.normalName().equals("mr-spoiler") || element.normalName().equals("mr-spoiler-block")) {
                        ignoreContent++;
                        text = element.hasAttr("title") ? element.attr("title") : "spoiler!";
                        text = "[" + text + "]";
                    } else if (element.normalName().equals("img")) {
                        text = EMOJI_PICTURE;
                    } else if (element.normalName().equals("blockquote") && collapseQuotations) {
                        text = EMOJI_SCROLL;
                        ignoreContent++;
                    } else if (
                        element.normalName().equals("iframe")
                        || element.normalName().equals("object")
                        || element.normalName().equals("video")
                    ) {
                        text = EMOJI_MOVIE;
                        ignoreContent++;
                    }
                }
                if (appendText(text)) {
                    return FilterResult.STOP;
                }
            }
            return FilterResult.CONTINUE;
        }

        private boolean appendText(String text) {
            if (!ObjectUtils.isEmpty(text)) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(text);
                if (result.length() >= len) {
                    Util.ellipsize(result, len);
                    return true;
                }
            }
            return false;
        }

        private String clear(String text) {
            return text.replace("\n", " ").replace("\r", "");
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            if (
                node instanceof Element element
                && (
                    element.normalName().equals("mr-spoiler")
                    || element.normalName().equals("mr-spoiler-block")
                    || element.normalName().equals("blockquote") && collapseQuotations
                    || element.normalName().equals("iframe")
                    || element.normalName().equals("object")
                    || element.normalName().equals("video")
                )
            ) {
                ignoreContent--;
            }
            return FilterResult.CONTINUE;
        }

    }

    private static String extractGalleryTexts(Body body, List<MediaFileOwner> media, int len) {
        if (ObjectUtils.isEmpty(media)) {
            return "";
        }

        StringBuilder heading = new StringBuilder();
        boolean hasGallery = false;
        Set<String> linkIds = MediaExtractor.extractMediaFileIds(body.getLinkPreviews());
        for (MediaFileOwner mediaFileOwner : media) {
            MediaFile mediaFile = mediaFileOwner.getMediaFile();
            if (mediaFile == null || linkIds.contains(mediaFile.getId())) {
                continue;
            }
            if (mediaFile.getRecognizedText() == null) {
                hasGallery = true;
                continue;
            }
            if (!heading.isEmpty()) {
                heading.append(' ');
            }
            heading.append(mediaFile.getRecognizedText());
            if (heading.length() >= len) {
                Util.ellipsize(heading, len);
                break;
            }
        }
        if (heading.length() < len && hasGallery) {
            heading.append(EMOJI_PICTURE);
        }
        return heading.toString();
    }

}
