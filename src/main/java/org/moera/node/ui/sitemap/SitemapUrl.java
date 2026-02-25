package org.moera.node.ui.sitemap;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SitemapRecord;
import org.moera.node.util.Util;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SitemapUrl {

    @JacksonXmlProperty(localName = "loc", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private String location;

    @JacksonXmlProperty(localName = "lastmod", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private String lastModified;

    @JacksonXmlProperty(localName = "changefreq", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private String changeFrequency;

    public SitemapUrl(String siteUrl, String location, String changeFrequency) {
        this.location = siteUrl + location;
        this.changeFrequency = changeFrequency;
    }

    public SitemapUrl(String siteUrl, SitemapRecord record) {
        location = siteUrl + "/post/" + record.getEntry().getId();
        lastModified = record.getModifiedAt().toInstant().toString();
        changeFrequency = roundedChangeFrequency(record);
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getChangeFrequency() {
        return changeFrequency;
    }

    public void setChangeFrequency(String changeFrequency) {
        this.changeFrequency = changeFrequency;
    }

}
