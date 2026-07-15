package org.moera.node.rest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.moera.lib.node.types.LinkPreviewInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.linkpreviewnet.LinkPreviewNet;
import org.moera.node.linkpreviewnet.LinkPreviewNetException;
import org.moera.node.linkpreviewnet.LinkPreviewNetInfo;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MediaManager;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/link-preview")
public class LinkPreviewController {

    private static final Logger log = LoggerFactory.getLogger(LinkPreviewController.class);

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(1);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private LinkPreviewNet linkPreviewNet;

    @Inject
    private MediaManager mediaManager;

    @GetMapping
    @Admin(Scope.OTHER)
    @Transactional
    public LinkPreviewInfo createLinkPreview(@RequestParam String url) {
        log.info("GET /link-preview, (url = {})", LogUtil.format(url));

        String host = URI.create(url).getHost();
        if (host == null) {
            throw new ObjectNotFoundFailure("link-preview.resource-not-found");
        }

        for (String domain : config.getLinkPreview().getDomains()) {
            if (!host.matches(domain)) {
                continue;
            }
            LinkPreview linkPreview = switch (config.getLinkPreview().getService()) {
                case "linkpreviewnet" -> queryLinkPreviewNet(url);
                default -> {
                    log.error("Unknown link preview service: " + LogUtil.format(config.getLinkPreview().getService()));
                    throw new OperationFailure("server.misconfiguration");
                }
            };
            downloadImage(linkPreview, url);
            return linkPreview.info();
        }

        LinkPreview linkPreview = queryDirectly(url);
        downloadImage(linkPreview, url);
        return linkPreview.info();
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
            .header(HttpHeaders.USER_AGENT, config.getUserAgent("link preview"))
            .followRedirects(true)
            .timeout((int) REQUEST_TIMEOUT.toMillis())
            .get();
    }

    private LinkPreview queryDirectly(String url) {
        Document document;
        try {
            document = fetchDocument(url);
        } catch (IOException e) {
            throw new ObjectNotFoundFailure("link-preview.resource-not-found");
        }

        LinkPreviewInfo linkPreviewInfo = new LinkPreviewInfo();
        linkPreviewInfo.setUrl(url);
        var linkPreview = new LinkPreview(linkPreviewInfo, null);

        Elements elements = document.select("meta");
        for (Element element : elements) {
            String property = element.attr("property");
            if (ObjectUtils.isEmpty(property)) {
                property = element.attr("name");
            }
            linkPreview = analyzeMetaTag(linkPreview, property, element.attr("content"));
        }
        if (ObjectUtils.isEmpty(linkPreviewInfo.getTitle())) {
            Elements titles = document.select("head title");
            for (Element title : titles) {
                linkPreviewInfo.setTitle(title.ownText());
            }
        }
        return linkPreview;
    }

    private static LinkPreview analyzeMetaTag(LinkPreview linkPreview, String property, String content) {
        switch (property) {
            case "og:site_name":
                linkPreview.info().setSiteName(content);
                return linkPreview;
            case "og:url":
                linkPreview.info().setUrl(content);
                return linkPreview;
            case "og:title":
                linkPreview.info().setTitle(content);
                return linkPreview;
            case "og:description":
                linkPreview.info().setDescription(content);
                return linkPreview;
            case "og:image":
                return linkPreview.withImageUrl(content);
            case "article:published_time":
                Long publishedAt = parseTimestamp(content);
                if (publishedAt != null) {
                    linkPreview.info().setPublishedAt(publishedAt);
                }
                return linkPreview;
            case "twitter:title":
            case "title":
                if (ObjectUtils.isEmpty(linkPreview.info().getTitle())) {
                    linkPreview.info().setTitle(content);
                }
                return linkPreview;
            case "twitter:description":
            case "description":
                if (ObjectUtils.isEmpty(linkPreview.info().getDescription())) {
                    linkPreview.info().setDescription(content);
                }
                return linkPreview;
            case "twitter:image":
                if (ObjectUtils.isEmpty(linkPreview.imageUrl())) {
                    return linkPreview.withImageUrl(content);
                }
                return linkPreview;
            default:
                return linkPreview;
        }
    }

    static Long parseTimestamp(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant().getEpochSecond();
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LinkPreview queryLinkPreviewNet(String url) {
        LinkPreviewNetInfo info;
        try {
            info = linkPreviewNet.query(url);
        } catch (LinkPreviewNetException e) {
            throw new OperationFailure("link-preview.request-failed");
        }

        LinkPreviewInfo linkPreviewInfo = new LinkPreviewInfo();
        linkPreviewInfo.setUrl(info.getUrl());
        linkPreviewInfo.setTitle(info.getTitle());
        linkPreviewInfo.setDescription(info.getDescription());
        return new LinkPreview(linkPreviewInfo, info.getImage());
    }

    private void downloadImage(LinkPreview linkPreview, String requestedUrl) {
        String imageUrl = resolveImageUrl(linkPreview.imageUrl(), linkPreview.info().getUrl(), requestedUrl);
        if (imageUrl == null) {
            return;
        }

        try {
            var mediaFileOwner = mediaManager.ownMedia(null, null, null, null, imageUrl, true, null);
            var image = PrivateMediaFileInfoUtil.build(
                mediaFileOwner,
                config.getMedia().getDirectServe(),
                new MediaGrantGenerator(requestContext.getOptions())
            );
            if (!Boolean.TRUE.equals(image.getAttachment())) {
                linkPreview.info().setImage(image);
            }
        } catch (Exception e) {
            log.debug("Failed to download link preview image {}: {}", LogUtil.format(imageUrl), e.getMessage());
        }
    }

    private static String resolveImageUrl(String imageUrl, String pageUrl, String requestedUrl) {
        if (ObjectUtils.isEmpty(imageUrl)) {
            return null;
        }

        String baseUrl = !ObjectUtils.isEmpty(pageUrl) ? pageUrl : requestedUrl;
        try {
            return UriUtil.resolve(imageUrl, baseUrl);
        } catch (IllegalArgumentException e) {
            return imageUrl;
        }
    }

    private record LinkPreview(LinkPreviewInfo info, String imageUrl) {

        private LinkPreview withImageUrl(String imageUrl) {
            return new LinkPreview(info, imageUrl);
        }

    }

}
