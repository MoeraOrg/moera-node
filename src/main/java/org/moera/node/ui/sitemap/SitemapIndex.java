package org.moera.node.ui.sitemap;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.moera.node.data.Sitemap;

@JacksonXmlRootElement(localName = "sitemapindex", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapIndex {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "sitemap")
    private List<SitemapIndexItem> items;

    public SitemapIndex(String siteUrl, Collection<Sitemap> sitemaps) {
        items = sitemaps.stream().map(m -> new SitemapIndexItem(siteUrl, m)).collect(Collectors.toList());
    }

    public List<SitemapIndexItem> getItems() {
        return items;
    }

    public void setItems(List<SitemapIndexItem> items) {
        this.items = items;
    }

}
