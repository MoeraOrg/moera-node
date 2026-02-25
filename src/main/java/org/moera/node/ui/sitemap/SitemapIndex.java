package org.moera.node.ui.sitemap;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonRootName;
import org.moera.node.data.Sitemap;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName(value = "sitemapindex", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
public class SitemapIndex {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "sitemap", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    private List<SitemapIndexItem> items;

    public SitemapIndex(String siteUrl, Collection<Sitemap> sitemaps, Instant earliestModified) {
        items = sitemaps.stream()
                .map(m -> new SitemapIndexItem(siteUrl, m, earliestModified))
                .collect(Collectors.toList());
    }

    public List<SitemapIndexItem> getItems() {
        return items;
    }

    public void setItems(List<SitemapIndexItem> items) {
        this.items = items;
    }

}
