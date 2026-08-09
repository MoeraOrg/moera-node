package org.moera.node.ui.sitemap;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

class SitemapSerializationTest {

    @Test
    void sitemapIndexItemsRemainMutable() {
        SitemapIndex index = new SitemapIndex("https://example.org", List.of(), Instant.EPOCH);

        index.items().add(new SitemapIndexItem("https://example.org", "/sitemaps/static", Instant.EPOCH));

        Assertions.assertEquals(1, index.items().size());
    }

    @Test
    void serializesRecordComponentsWithSitemapXmlNames() {
        SitemapUrlSet urlSet = new SitemapUrlSet(List.of(
            SitemapUrl.staticPage("https://example.org", "/about", "monthly")
        ));

        String xml = new XmlMapper().writeValueAsString(urlSet);

        Assertions.assertTrue(xml.contains("<urlset"));
        Assertions.assertTrue(xml.contains("<url>"));
        Assertions.assertTrue(xml.contains("<loc>https://example.org/about</loc>"));
        Assertions.assertTrue(xml.contains("<changefreq>monthly</changefreq>"));
        Assertions.assertFalse(xml.contains("<lastmod>"));
    }

}
