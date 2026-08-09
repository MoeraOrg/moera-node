package org.moera.node.ui.sitemap;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRootName;
import org.moera.node.data.SitemapRecord;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName(value = "urlset", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
public record SitemapUrlSet(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "url", namespace = org.moera.node.ui.sitemap.Sitemap.NAMESPACE)
    List<SitemapUrl> items
) {

    public SitemapUrlSet(String siteUrl, Collection<SitemapRecord> records) {
        this(records.stream().map(r -> new SitemapUrl(siteUrl, r)).toList());
    }

}
