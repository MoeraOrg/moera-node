package org.moera.node.text.sanitizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamEventReceiverWrapper;

class ImageProcessor extends HtmlStreamEventReceiverWrapper {

    private final Map<String, MediaFileOwner> media;

    ImageProcessor(HtmlStreamEventReceiver underlying, List<MediaFileOwner> mediaFileOwners) {
        super(underlying);

        media = mediaFileOwners.stream()
                .collect(Collectors.toMap(mfo -> mfo.getMediaFile().getId(), Function.identity()));
    }

    private String mediaPreview(String location, int width) {
        return String.format("%s?width=%d", location, width);
    }

    private String mediaSources(String location, Collection<MediaFilePreview> previews) {
        List<String> sources = new ArrayList<>();
        for (MediaFilePreview preview : previews) {
            if (preview.getMediaFile() == null) {
                continue;
            }
            String url = preview.isOriginal() ? location : mediaPreview(location, preview.getWidth());
            sources.add(String.format("%s %dw", url,
                    preview.getMediaFile().getSizeX()));
        }
        return String.join(",", sources);
    }

    private String mediaSizes(MediaFile mediaFile) {
        return String.format("(max-width: 400px) %dpx, %dpx",
                Math.min(350, mediaFile.findLargerPreview(350).getMediaFile().getSizeX()),
                Math.min(900, mediaFile.findLargerPreview(900).getMediaFile().getSizeX()));
    }

    @Override
    public void openTag(String elementName, List<String> attrs) {
        if (elementName.equalsIgnoreCase("img")) {
            List<String> newAttrs = new ArrayList<>();
            MediaFileOwner mediaFileOwner = null;
            Integer width = null;
            Integer height = null;
            for (int i = 0; i < attrs.size(); i += 2) {
                String attrName = attrs.get(i);
                String attrValue = attrs.get(i + 1);
                if (attrName.equalsIgnoreCase("src")) {
                    if (!attrValue.startsWith("hash:")) {
                        break;
                    }
                    mediaFileOwner = media.get(attrValue.substring(5));
                    if (mediaFileOwner == null) {
                        break;
                    }
                } else if (attrName.equalsIgnoreCase("width")) {
                    try {
                        width = Integer.parseInt(attrValue);
                    } catch (NumberFormatException e) {
                        // leave as null
                    }
                } else if (attrName.equalsIgnoreCase("height")) {
                    try {
                        height = Integer.parseInt(attrValue);
                    } catch (NumberFormatException e) {
                        // leave as null
                    }
                } else {
                    newAttrs.add(attrName);
                    newAttrs.add(attrValue);
                }
            }
            if (mediaFileOwner == null) {
                super.openTag(elementName, attrs);
                return;
            }

            String mediaLocation = "/moera/media/private/" + mediaFileOwner.getFileName();

            super.openTag("a", new ArrayList<>(List.of(
                    "href", mediaLocation,
                    "class", "entry-image",
                    "data-id", mediaFileOwner.getId().toString()
            )));

            newAttrs.add("src");
            newAttrs.add(mediaPreview(mediaLocation, 900));
            newAttrs.add("srcset");
            newAttrs.add(mediaSources(mediaLocation, mediaFileOwner.getMediaFile().getPreviews()));
            newAttrs.add("sizes");
            newAttrs.add(mediaSizes(mediaFileOwner.getMediaFile()));

            width = width == null || width == 0 ? null : width;
            height = height == null || height == 0 ? null : height;
            MediaFile mediaFile = mediaFileOwner.getMediaFile().findLargerPreview(900).getMediaFile();
            Integer sizeX = mediaFile.getSizeX();
            Integer sizeY = mediaFile.getSizeY();
            double scale;
            if (width == null && height == null) {
                scale = 1;
            } else {
                double scaleX = width != null ? (double) width / sizeX : 1;
                double scaleY = height != null ? (double) height / sizeY : 1;
                scale = Math.min(scaleX, scaleY);
            }

            newAttrs.add("width");
            newAttrs.add(Long.toString(Math.round(scale * sizeX)));
            newAttrs.add("height");
            newAttrs.add(Long.toString(Math.round(scale * sizeY)));
            super.openTag(elementName, newAttrs);

            super.closeTag("a");

            return;
        }
        super.openTag(elementName, attrs);
    }

}