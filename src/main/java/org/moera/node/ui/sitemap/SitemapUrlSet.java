package org.moera.node.ui.sitemap;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonRootName;
import org.moera.node.data.SitemapRecord;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName(value = "urlset", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
public class SitemapUrlSet {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "url", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private List<SitemapUrl> items;

    public SitemapUrlSet(List<SitemapUrl> items) {
        this.items = items;
    }

    public SitemapUrlSet(String siteUrl, Collection<SitemapRecord> records) {
        items = records.stream().map(r -> new SitemapUrl(siteUrl, r)).collect(Collectors.toList());
    }

    public List<SitemapUrl> getItems() {
        return items;
    }

    public void setItems(List<SitemapUrl> items) {
        this.items = items;
    }

}
