package org.moera.node.ui.sitemap;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SitemapRecord;
import org.moera.node.util.Util;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SitemapUrl(
    @JacksonXmlProperty(localName = "loc", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    String location,
    @JacksonXmlProperty(localName = "lastmod", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    String lastModified,
    @JacksonXmlProperty(localName = "changefreq", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    String changeFrequency
) {

    public SitemapUrl(String siteUrl, SitemapRecord record) {
        this(
            siteUrl + "/post/" + record.getEntry().getId(),
            record.getModifiedAt().toInstant().toString(),
            roundedChangeFrequency(record)
        );
    }

    public static SitemapUrl staticPage(String siteUrl, String location, String changeFrequency) {
        return new SitemapUrl(siteUrl + location, null, changeFrequency);
    }

    private static String roundedChangeFrequency(SitemapRecord record) {
        long time = Instant.now().toEpochMilli() / 1000 - Util.toEpochSecond(record.getModifiedAt());
        float period = (float) time / record.getTotalUpdates() / (3600 * 24);
        if (period < 1) {
            return "hourly";
        } else if (period < 7) {
            return "daily";
        } else if (period < 30) {
            return "weekly";
        } else if (period < 365) {
            return "monthly";
        } else {
            return "yearly";
        }
    }

}
