package org.moera.node.ui.sitemap;

import java.time.Instant;

import org.moera.node.data.Sitemap;
import org.moera.node.util.Util;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record SitemapIndexItem(
    @JacksonXmlProperty(localName = "loc", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    String location,
    @JacksonXmlProperty(localName = "lastmod", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    String lastModified
) {

    public SitemapIndexItem(String siteUrl, String location, Instant lastModified) {
        this(siteUrl + location, (lastModified != null ? lastModified : Instant.EPOCH).toString());
    }

    public SitemapIndexItem(String siteUrl, Sitemap sitemap, Instant earliestModified) {
        this(
            siteUrl + "/sitemaps/" + sitemap.getId(),
            Util.latest(sitemap.getModifiedAt().toInstant(), earliestModified).toString()
        );
    }

}
