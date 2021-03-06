package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.operations.CommentPublicPageOperations;
import org.moera.node.operations.TimelinePublicPageOperations;
import org.moera.node.util.VirtualPageHeader;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@UiController
public class TimelineUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

    @Inject
    private PublicPageRepository publicPageRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    @Inject
    private CommentPublicPageOperations commentPublicPageOperations;

    @GetMapping("/timeline")
    @VirtualPage
    public String timeline(@RequestParam(required = false) Long before, HttpServletResponse response, Model model) {
        before = before != null ? before : Long.MAX_VALUE;
        List<StoryInfo> stories = Collections.emptyList();
        PublicPage publicPage = publicPageRepository.findContaining(requestContext.nodeId(), before);
        if (publicPage != null) {
            stories = storyRepository.findInRange(
                    requestContext.nodeId(), Feed.TIMELINE, publicPage.getAfterMoment(), publicPage.getBeforeMoment())
                    .stream()
                    .filter(t -> t.getEntry().isMessage())
                    .map(s -> StoryInfo.build(s, false, t -> PostingInfo.forUi((Posting) t.getEntry())))
                    .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                    .collect(Collectors.toList());
        }

        model.addAttribute("pageTitle", titleBuilder.build("Timeline"));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("anchor", "m" + before);
        model.addAttribute("stories", stories);
        model.addAttribute("pagination", timelinePublicPageOperations.createPagination(publicPage));

        return "timeline";
    }

    @GetMapping("/post/{id}")
    public String posting(@PathVariable UUID id, @RequestParam(required = false) Long before,
                          @RequestParam(name = "comment", required = false) UUID commentId,
                          HttpServletResponse response, Model model) {
        if (commentId != null) {
            VirtualPageHeader.put(response, String.format("/post/%s?comment=%s", id, commentId));
        } else {
            VirtualPageHeader.put(response, String.format("/post/%s", id));
        }
        if (requestContext.isBrowserExtension()) {
            return null;
        }

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null || !posting.isMessage()) {
            throw new PageNotFoundException();
        }
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);

        model.addAttribute("pageTitle", titleBuilder.build(posting.getCurrentRevision().getHeading()));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("posting", PostingInfo.forUi(posting, stories));
        model.addAttribute("openComments", commentId != null || before != null);

        if (posting.isOriginal()) {
            Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                    .orElse(null);
            before = comment != null ? comment.getMoment() : before;
            before = before != null ? before : Long.MIN_VALUE + 1;
            List<CommentInfo> comments = Collections.emptyList();
            PublicPage publicPage = publicPageRepository.findContainingForEntry(requestContext.nodeId(), id, before);
            if (publicPage != null) {
                comments = commentRepository.findInRange(
                        requestContext.nodeId(), id, publicPage.getAfterMoment(), publicPage.getBeforeMoment())
                        .stream()
                        .filter(Comment::isMessage)
                        .map(CommentInfo::forUi)
                        .sorted(Comparator.comparing(CommentInfo::getMoment))
                        .collect(Collectors.toList());
            }

            if (commentId != null) {
                model.addAttribute("anchor", "comment-" + commentId);
            }
            model.addAttribute("comments", comments);
            model.addAttribute("commentId", Objects.toString(commentId, null));
            model.addAttribute("pagination", commentPublicPageOperations.createPagination(publicPage));
        } else {
            model.addAttribute("originalHref", NamingCache.getRedirector(posting.getReceiverName(),
                    String.format("/post/%s", posting.getReceiverEntryId())));
            String location = commentId != null
                    ? String.format("/post/%s?comment=%s", posting.getReceiverEntryId(), commentId)
                    : String.format("/post/%s", posting.getReceiverEntryId());
            model.addAttribute("commentsHref", NamingCache.getRedirector(posting.getReceiverName(), location));
        }

        return "posting";
    }

}
