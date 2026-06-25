package org.moera.node.text.sanitizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.node.data.MediaFile;
import org.moera.node.global.ServeContext;
import org.moera.node.media.LocalRemoteMedia;
import org.moera.node.media.MediaUtil;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamEventReceiverWrapper;

class ImageProcessor extends HtmlStreamEventReceiverWrapper {

    private final ServeContext serveContext;
    private final Map<String, LocalRemoteMedia> media;

    ImageProcessor(
        ServeContext serveContext,
        HtmlStreamEventReceiver underlying,
        List<LocalRemoteMedia> mediaAttachments
    ) {
        super(underlying);

        this.serveContext = serveContext;
        media = mediaAttachments != null
                ? mediaAttachments.stream()
                    .collect(
                        Collectors.toMap(
                            LocalRemoteMedia::hash,
                            Function.identity(),
                            (mfo1, mfo2) -> mfo1
                        )
                    )
                : Collections.emptyMap();
    }

    @Override
    public void openTag(String elementName, List<String> attrs) {
        if (elementName.equalsIgnoreCase("img")) {
            List<String> newAttrs = new ArrayList<>();
            LocalRemoteMedia localRemoteMedia = null;
            String src = null;
            Integer width = null;
            Integer height = null;
            for (int i = 0; i < attrs.size(); i += 2) {
                String attrName = attrs.get(i);
                String attrValue = attrs.get(i + 1);
                if (attrName.equalsIgnoreCase("src")) {
                    src = attrValue;
                    if (attrValue.startsWith("hash:")) {
                        localRemoteMedia = media.get(attrValue.substring(5));
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
            if (
                localRemoteMedia == null
                || localRemoteMedia.mediaFileOwner() == null && localRemoteMedia.remoteMediaFile() == null
            ) {
                if (width == null && height == null) {
                    super.openTag(elementName, attrs);
                    return;
                }

                if (src != null) {
                    newAttrs.add("src");
                    newAttrs.add(src);
                }
                String style = "";
                if (width != null) {
                    newAttrs.add("width");
                    newAttrs.add(Integer.toString(width));
                    style += "; --width: %spx".formatted(width);
                }
                if (height != null) {
                    newAttrs.add("height");
                    newAttrs.add(Integer.toString(height));
                    style += "; --height: %spx".formatted(height);
                }
                newAttrs.add("style");
                newAttrs.add(style);
                super.openTag(elementName, newAttrs);
                return;
            }

            String mediaLocation = localRemoteMedia.path(serveContext, null);
            String mediaId = localRemoteMedia.mediaId();
            super.openTag("a", new ArrayList<>(List.of(
                "href", mediaLocation != null ? mediaLocation : "",
                "class", "entry-image",
                "data-id", mediaId != null ? mediaId : ""
            )));

            newAttrs.add("src");
            String mediaSrc = localRemoteMedia.path(serveContext, 900);
            newAttrs.add(mediaSrc != null ? mediaSrc : "");
            if (localRemoteMedia.mediaFileOwner() != null) {
                newAttrs.add("srcset");
                newAttrs.add(
                    MediaUtil.mediaSources(
                        mediaLocation,
                        localRemoteMedia.mediaFileOwner(),
                        serveContext.directServeConfig()
                    )
                );
                newAttrs.add("sizes");
                newAttrs.add(MediaUtil.mediaSizes(localRemoteMedia.mediaFileOwner().getMediaFile()));
            }

            width = width == null || width == 0 ? null : width;
            height = height == null || height == 0 ? null : height;
            Integer sizeX;
            Integer sizeY;
            if (localRemoteMedia.mediaFileOwner() != null) {
                MediaFile mediaFile =
                    localRemoteMedia.mediaFileOwner().getMediaFile().findLargerPreview(900).getMediaFile();
                sizeX = mediaFile.getSizeX();
                sizeY = mediaFile.getSizeY();
            } else {
                sizeX = localRemoteMedia.remoteMediaFile().getSizeX();
                sizeY = localRemoteMedia.remoteMediaFile().getSizeY();
                sizeX = sizeX != null ? sizeX : 900;
                sizeY = sizeY != null ? sizeY : 900;
                double scaleX = 900.0 / sizeX;
                double scaleY = 900.0 / sizeY;
                if (scaleX < 1 || scaleY < 1) {
                    double scale = Math.min(scaleX, scaleY);
                    sizeX = (int) Math.round(scale * sizeX);
                    sizeY = (int) Math.round(scale * sizeY);
                }
            }
            double scale;
            if (width == null && height == null) {
                scale = 1;
            } else {
                double scaleX = width != null ? (double) width / sizeX : 1;
                double scaleY = height != null ? (double) height / sizeY : 1;
                scale = Math.min(scaleX, scaleY);
            }
            String imageWidth = Long.toString(Math.round(scale * sizeX));
            String imageHeight = Long.toString(Math.round(scale * sizeY));

            newAttrs.add("width");
            newAttrs.add(imageWidth);
            newAttrs.add("height");
            newAttrs.add(imageHeight);
            newAttrs.add("style");
            newAttrs.add("--width: %spx; --height: %spx".formatted(imageWidth, imageHeight));
            super.openTag(elementName, newAttrs);

            super.closeTag("a");

            return;
        }
        super.openTag(elementName, attrs);
    }

}
