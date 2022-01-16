package org.moera.node.ui.sitemap;

import java.time.Instant;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.moera.node.data.Sitemap;

public class SitemapIndexItem {

    @JacksonXmlProperty(localName = "loc")
    private String location;

    @JacksonXmlProperty(localName = "lastmod")
    private String lastModified;

    public SitemapIndexItem(String siteUrl, String location, Instant lastModified) {
        this.location = siteUrl + location;
        this.lastModified = lastModified.toString();
    }

    public SitemapIndexItem(String siteUrl, Sitemap sitemap) {
        location = siteUrl + "/sitemaps/" + sitemap.getId();
        lastModified = sitemap.getModifiedAt().toInstant().toString();
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
