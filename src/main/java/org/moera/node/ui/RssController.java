package org.moera.node.ui;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RssController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private PublicPageRepository publicPageRepository;

    @Inject
    private PostingRepository postingRepository;

    @GetMapping("/rss")
    @ResponseBody
    public SyndFeed rss() {
        RequestContext rcp = requestContext.getPublic();
        PublicPage publicPage = publicPageRepository.findContaining(rcp.nodeId(), Long.MAX_VALUE);
        List<Posting> postings = Collections.emptyList();
        if (publicPage != null) {
            postings = postingRepository.findInRange(
                    rcp.nodeId(), publicPage.getAfterMoment(), publicPage.getBeforeMoment());
        }

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        String name = rcp.getOptions().getString("profile.registered-name");
        String title = !StringUtils.isEmpty(name) ? name + " - Moera" : "Moera";
        feed.setTitle(title);
        feed.setLink(rcp.getSiteUrl() + "/");
        feed.setDescription(title);
        feed.setLanguage("en-us");
        feed.setPublishedDate(!postings.isEmpty() ? postings.get(0).getCreatedAt() : Util.now());
        feed.setGenerator("moera-node");

        feed.setEntries(postings.stream().map(this::buildEntry).collect(Collectors.toList()));

        return feed;
    }

    private SyndEntry buildEntry(Posting posting) {
        String siteUrl = requestContext.getSiteUrl();

        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(posting.getHeading());
        entry.setLink(siteUrl + "/post/" + posting.getEntryId());
        entry.setUri("urn:entry:" + posting.getEntryId());
        entry.setPublishedDate(posting.getPublishedAt());

        StringBuilder buf = new StringBuilder();
        boolean hasPreview = !StringUtils.isEmpty(posting.getBodyPreviewHtml());
        buf.append("<div>");
        buf.append(hasPreview ? posting.getBodyPreviewHtml() : posting.getBodyHtml());
        buf.append("</div>");
        if (hasPreview) {
            buf.append(String.format("<a href=\"/post/%s\">Continue Reading &rarr;</a>", posting.getEntryId()));
        }

        SyndContent content = new SyndContentImpl();
        content.setType("text/html");
        content.setValue(buf.toString());

        entry.setDescription(content);

        return entry;
    }

}