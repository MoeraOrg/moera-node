package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.feed.synd.SyndImageImpl;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AvatarImage;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;
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
    private StoryRepository storyRepository;

    @GetMapping("/rss")
    @ResponseBody
    public SyndFeed rss() {
        RequestContext rcp = requestContext.getPublic();
        PublicPage publicPage = publicPageRepository.findContaining(rcp.nodeId(), Long.MAX_VALUE);
        List<Story> stories = Collections.emptyList();
        if (publicPage != null) {
            stories = storyRepository.findInRange(
                    rcp.nodeId(), Feed.TIMELINE, publicPage.getAfterMoment(), publicPage.getBeforeMoment()).stream()
                    .filter(t -> t.getEntry().isMessage())
                    .sorted(Collections.reverseOrder(Comparator.comparingLong(Story::getMoment)))
                    .collect(Collectors.toList());
        }

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        String name = rcp.fullName() != null ? rcp.fullName() : rcp.nodeName();
        String title = !ObjectUtils.isEmpty(name) ? name + " - Moera" : "Moera";
        feed.setTitle(title);
        feed.setLink(rcp.getSiteUrl() + "/");
        if (rcp.avatarId() != null) {
            SyndImage image = new SyndImageImpl();
            image.setTitle(title);
            image.setUrl(rcp.getSiteUrl() + "/moera/media/" + new AvatarImage(rcp.getAvatar()).getPath());
            image.setLink(rcp.getSiteUrl() + "/");
            feed.setImage(image);
        }
        feed.setDescription(title);
        feed.setLanguage("en-us");
        feed.setPublishedDate(!stories.isEmpty() ? stories.get(0).getCreatedAt() : Util.now());
        feed.setGenerator("moera-node");
        feed.setWebMaster(buildWebmaster());

        feed.setEntries(stories.stream().map(this::buildEntry).collect(Collectors.toList()));

        return feed;
    }

    private String buildWebmaster() {
        String name = requestContext.getOptions().getString("webmaster.name");
        String email = requestContext.getOptions().getString("webmaster.email");
        if (ObjectUtils.isEmpty(name)) {
            if (ObjectUtils.isEmpty(email)) {
                return null;
            } else {
                return email;
            }
        } else {
            if (ObjectUtils.isEmpty(email)) {
                return name;
            } else {
                return String.format("%s (%s)", email, name);
            }
        }
    }

    private SyndEntry buildEntry(Story story) {
        Entry posting = story.getEntry();
        EntryRevision revision = posting.getCurrentRevision();
        String siteUrl = requestContext.getSiteUrl();

        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(revision.getHeading());
        entry.setLink(siteUrl + "/post/" + posting.getId());
        entry.setUri("urn:entry:" + posting.getId());
        entry.setPublishedDate(story.getPublishedAt());

        StringBuilder buf = new StringBuilder();
        boolean hasPreview = !ObjectUtils.isEmpty(revision.getBodyPreview());
        buf.append("<div>");
        buf.append(hasPreview ? revision.getBodyPreview() : revision.getBody());
        buf.append("</div>");
        if (hasPreview) {
            buf.append(String.format("<a href=\"/post/%s\">Continue Reading &rarr;</a>", posting.getId()));
        }

        SyndContent content = new SyndContentImpl();
        content.setType("text/html");
        content.setValue(buf.toString());

        entry.setDescription(content);

        return entry;
    }

}
