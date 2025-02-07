package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.moera.lib.UniversalLocation;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
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
import org.moera.node.model.AvatarImage;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.operations.CommentPublicPageOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.TimelinePublicPageOperations;
import org.moera.node.util.VirtualPageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@UiController
public class TimelineUiController {

    private static final Logger log = LoggerFactory.getLogger(TimelineUiController.class);

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
    private EntryOperations entryOperations;

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    @Inject
    private CommentPublicPageOperations commentPublicPageOperations;

    @Inject
    private NamingCache namingCache;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/timeline", produces = "text/html")
    @VirtualPage
    @Transactional
    public String timeline(@RequestParam(required = false) Long before, Model model) {
        log.info("UI /timeline (before = {})", LogUtil.format(before));

        String canonicalUrl = "/timeline" + (before != null ? "?before=" + before : "");

        before = before != null ? before : Long.MAX_VALUE;
        List<StoryInfo> stories = Collections.emptyList();
        PublicPage publicPage = publicPageRepository.findContaining(requestContext.nodeId(), before);
        if (publicPage != null) {
            stories = storyRepository.findInRange(
                    requestContext.nodeId(), Feed.TIMELINE, publicPage.getAfterMoment(), publicPage.getBeforeMoment())
                    .stream()
                    .filter(t -> t.getEntry() != null)
                    .filter(t -> t.getEntry().isMessage())
                    .filter(t -> t.getEntry().getViewCompound().isPublic())
                    .map(s -> StoryInfo.build(s, false, t -> PostingInfo.forUi(t.getEntry(), entryOperations)))
                    .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                    .toList();
        }

        model.addAttribute("pageTitle", titleBuilder.build("Timeline"));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("anchor", "m" + before);
        model.addAttribute("stories", stories);
        model.addAttribute("pagination", timelinePublicPageOperations.createPagination(publicPage));

        model.addAttribute("ogUrl", requestContext.getSiteUrl());

        model.addAttribute("noIndex", true);

        return "timeline";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/post/{id}", produces = "text/html")
    @Transactional
    public String posting(@PathVariable UUID id, @RequestParam(required = false) Long before,
                          @RequestParam(name = "comment", required = false) UUID commentId,
                          @RequestParam(name = "media", required = false) UUID mediaId,
                          HttpServletResponse response, Model model) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/post/" + id);
        if (commentId != null) {
            builder = builder.queryParam("comment", commentId);
        }
        String canonicalUrl = builder.build().toUriString();
        if (mediaId != null) {
            builder = builder.queryParam("media", mediaId);
        }
        VirtualPageHeader.put(response, requestContext.nodeName(), builder.build().toUriString());
        if (requestContext.isAutoClient()) {
            return "redirect:" + requestContext.getRedirectorUrl();
        }

        log.info("UI /post/{id} (id = {}, before = {}, comment = {}, media = {})",
                LogUtil.format(id), LogUtil.format(before), LogUtil.format(commentId), LogUtil.format(mediaId));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null || !posting.isMessage() || posting.getParentMedia() != null
                || !posting.getViewCompound().isPublic()) {
            throw new PageNotFoundException();
        }
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);

        model.addAttribute("pageTitle", titleBuilder.build(posting.getCurrentRevision().getHeading()));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("posting",
                PostingInfo.forUi(posting, stories, entryOperations, requestContext.getOptions()));
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("openComments", commentId != null || before != null);
        model.addAttribute("openMediaPostingId", id.toString());
        model.addAttribute("openMediaCommentId", Objects.toString(commentId, null));
        model.addAttribute("openMediaId", Objects.toString(mediaId, null));

        Comment comment = commentId != null && posting.isOriginal()
                ? commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null)
                : null;
        if (posting.isOriginal()) {
            if (posting.getViewCommentsCompound().isPublic()) {
                before = comment != null ? comment.getMoment() : before;
                before = before != null ? before : Long.MIN_VALUE + 1;
                List<CommentInfo> comments = Collections.emptyList();
                PublicPage publicPage = publicPageRepository.findContainingForEntry(requestContext.nodeId(), id, before);
                if (publicPage != null && posting.getViewCommentsCompound().isPublic()) {
                    comments = commentRepository.findInRange(requestContext.nodeId(), id,
                                    publicPage.getAfterMoment(), publicPage.getBeforeMoment())
                            .stream()
                            .filter(Comment::isMessage)
                            .filter(c -> c.getViewCompound().isPublic())
                            .map(c -> CommentInfo.forUi(c, entryOperations))
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
                model.addAttribute("comments", Collections.emptyList());
            }
        } else {
            model.addAttribute("noIndex", true);
            model.addAttribute("originalHref",
                    entryLocation(posting.getReceiverName(), null, posting.getReceiverEntryId(), null));
            model.addAttribute("commentsHref",
                    entryLocation(posting.getReceiverName(), null, posting.getReceiverEntryId(), commentId));
        }

        model.addAttribute("ogUrl",
                entryLocation(requestContext.nodeName(), requestContext.getSiteUrl(), posting.getId(), commentId));
        model.addAttribute("ogType", "article");
        Entry entry = comment != null ? comment : posting;
        String heading = entry.getCurrentRevision().getHeading();
        model.addAttribute("ogTitle", !ObjectUtils.isEmpty(heading) ? heading : "(no title)");
        MediaAttachment[] attachments = entryOperations.getMediaAttachments(entry.getCurrentRevision(), null);
        PrivateMediaFileInfo image = attachments.length > 0 ? attachments[0].getMedia() : null;
        if (image != null) {
            model.addAttribute("ogImage", requestContext.getSiteUrl() + "/moera/media/" + image.getPath());
            model.addAttribute("ogImageType", image.getMimeType());
            model.addAttribute("ogImageWidth", image.getWidth());
            model.addAttribute("ogImageHeight", image.getHeight());
        } else if (entry.getOwnerAvatarMediaFile() != null) {
            AvatarImage avatarImage = new AvatarImage(entry.getOwnerAvatarMediaFile(), entry.getOwnerAvatarShape());
            model.addAttribute("ogImage", requestContext.getSiteUrl() + "/moera/media/" + avatarImage.getPath());
            model.addAttribute("ogImageType", avatarImage.getMediaFile().getMimeType());
            model.addAttribute("ogImageWidth", avatarImage.getWidth());
            model.addAttribute("ogImageHeight", avatarImage.getHeight());
        }
        String description = entry.getCurrentRevision().getDescription();
        description = !ObjectUtils.isEmpty(description) ? description : entry.getCurrentRevision().getHeading();
        model.addAttribute("ogDescription", description);
        var createdAt = entry.getReceiverCreatedAt() != null ? entry.getReceiverCreatedAt() : entry.getCreatedAt();
        model.addAttribute("ogArticlePublishedTime", createdAt.toInstant().toString());
        if (entry.getEditedAt() != null) {
            var editedAt = entry.getReceiverEditedAt() != null ? entry.getReceiverEditedAt() : entry.getEditedAt();
            model.addAttribute("ogArticleModifiedTime", editedAt.toInstant().toString());
        }

        return "posting";
    }

    private String entryLocation(String nodeName, String nodeUrl, Object postingId, Object commentId) {
        nodeUrl = nodeUrl != null ? nodeUrl : namingCache.getFast(nodeName).getNodeUri();
        String query = commentId != null ? "?comment=" + commentId : null;
        return UniversalLocation.redirectTo(nodeName, nodeUrl, "/post/" + postingId, query, null);
    }

}
