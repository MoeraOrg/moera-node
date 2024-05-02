package org.moera.node.ui.sitemap;

import java.time.Instant;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.moera.node.data.Sitemap;
import org.moera.node.util.Util;

public class SitemapIndexItem {

    @JacksonXmlProperty(localName = "loc", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private String location;

    @JacksonXmlProperty(localName = "lastmod", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private String lastModified;

    public SitemapIndexItem(String siteUrl, String location, Instant lastModified) {
        this.location = siteUrl + location;
        this.lastModified = lastModified.toString();
    }

    public SitemapIndexItem(String siteUrl, Sitemap sitemap, Instant earliestModified) {
        location = siteUrl + "/sitemaps/" + sitemap.getId();
        lastModified = Util.latest(sitemap.getModifiedAt().toInstant(), earliestModified).toString();
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

}
