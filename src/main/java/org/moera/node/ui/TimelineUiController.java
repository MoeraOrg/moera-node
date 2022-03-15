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

import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
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
import org.moera.node.model.body.Body;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.operations.CommentPublicPageOperations;
import org.moera.node.operations.TimelinePublicPageOperations;
import org.moera.node.util.VirtualPageHeader;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

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
    @Transactional
    public String timeline(@RequestParam(required = false) Long before, HttpServletResponse response, Model model) {
        String canonicalUrl = "/timeline" + (before != null ? "?before=" + before : "");

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
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("anchor", "m" + before);
        model.addAttribute("stories", stories);
        model.addAttribute("pagination", timelinePublicPageOperations.createPagination(publicPage));

        model.addAttribute("ogUrl", requestContext.getSiteUrl());

        return "timeline";
    }

    @GetMapping("/post/{id}")
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
        VirtualPageHeader.put(response, builder.build().toUriString());

        if (requestContext.isBrowserExtension()) {
            return null;
        }

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null || !posting.isMessage() || posting.getParentMedia() != null) {
            throw new PageNotFoundException();
        }
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);

        model.addAttribute("pageTitle", titleBuilder.build(posting.getCurrentRevision().getHeading()));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("posting", PostingInfo.forUi(posting, stories));
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("openComments", commentId != null || before != null);
        model.addAttribute("openMediaPostingId", id.toString());
        model.addAttribute("openMediaCommentId", commentId != null ? commentId.toString() : null);
        model.addAttribute("openMediaId", Objects.toString(mediaId, null));

        Comment comment = commentId != null && posting.isOriginal()
                ? commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null)
                : null;
        if (posting.isOriginal()) {
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
                    entryLocation(posting.getReceiverEntryId(), null)));
            model.addAttribute("commentsHref", NamingCache.getRedirector(posting.getReceiverName(),
                    entryLocation(posting.getReceiverEntryId(), commentId)));
        }

        model.addAttribute("ogUrl", requestContext.getSiteUrl() + entryLocation(posting.getId(), commentId));
        model.addAttribute("ogType", "article");
        Entry entry = comment != null ? comment : posting;
        String subject = new Body(entry.getCurrentRevision().getBody()).getSubject();
        model.addAttribute("ogTitle", !ObjectUtils.isEmpty(subject) ? subject : "(no title)");
        EntryAttachment attachment = entry.getCurrentRevision().getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .filter(ea -> mediaId == null || ea.getMediaFileOwner().getId().equals(mediaId))
                .filter(ea -> ea.getMediaFileOwner().getMediaFile().getSizeX() != null) // an image
                .findFirst()
                .orElse(null);
        if (attachment != null) {
            PrivateMediaFileInfo image =
                    new PrivateMediaFileInfo(attachment.getMediaFileOwner(), posting.getReceiverName());
            model.addAttribute("ogImage", requestContext.getSiteUrl() + "/moera/media/" + image.getPath());
            model.addAttribute("ogImageType", attachment.getMediaFileOwner().getMediaFile().getMimeType());
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

    private String entryLocation(Object postingId, Object commentId) {
        return commentId != null
                ? String.format("/post/%s?comment=%s", postingId, commentId)
                : String.format("/post/%s", postingId);
    }

}
