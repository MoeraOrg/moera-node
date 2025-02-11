package org.moera.node.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.SheriffOrderCategory;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.SheriffOrderFingerprintBuilder;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SheriffOrderReceivedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffOrderDetails;
import org.moera.node.model.ValidationFailure;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.operations.FeedOperations;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/sheriff/orders")
@NoCache
public class SheriffOrderController {

    private static final Logger log = LoggerFactory.getLogger(SheriffOrderController.class);

    private static final Duration CREATED_AT_MARGIN = Duration.ofMinutes(10);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private NamingCache namingCache;

    @PostMapping
    @Transactional
    public Result post(@Valid @RequestBody SheriffOrderDetails sheriffOrderDetails) {
        log.info("POST /sheriff/orders (delete = {}, sheriffName = {}, feedName = {}, postingId = {}, commentId = {},"
                        + " category = {}, reasonCode = {})",
                LogUtil.format(sheriffOrderDetails.isDelete()),
                LogUtil.format(sheriffOrderDetails.getSheriffName()),
                LogUtil.format(sheriffOrderDetails.getFeedName()),
                LogUtil.format(sheriffOrderDetails.getPostingId()),
                LogUtil.format(sheriffOrderDetails.getCommentId()),
                LogUtil.format(SheriffOrderCategory.toValue(sheriffOrderDetails.getCategory())),
                LogUtil.format(SheriffOrderReason.toValue(sheriffOrderDetails.getReasonCode())));

        if (Duration.between(Instant.ofEpochSecond(sheriffOrderDetails.getCreatedAt()), Instant.now()).abs()
                .compareTo(CREATED_AT_MARGIN) > 0) {
            throw new ValidationFailure("sheriffOrderDetails.createdAt.out-of-range");
        }

        mediaOperations.validateAvatar(
                sheriffOrderDetails.getSheriffAvatar(),
                sheriffOrderDetails::setSheriffAvatarMediaFile,
                () -> new ValidationFailure("sheriffOrderDetails.sheriffAvatar.mediaId.not-found"));

        Posting posting = null;
        Comment comment = null;
        byte[] entryDigest = null;
        if (sheriffOrderDetails.getPostingId() != null) {
            posting = postingRepository.findFullByNodeIdAndId(
                            requestContext.nodeId(), sheriffOrderDetails.getPostingId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            if (posting.getCurrentRevision().getSignature() == null) {
                throw new ValidationFailure("posting.not-signed");
            }
            List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());
            boolean inFeed = stories.stream()
                    .anyMatch(story -> story.getFeedName().equals(sheriffOrderDetails.getFeedName()));
            if (!inFeed) {
                throw new ValidationFailure("sheriff-order.wrong-feed");
            }
            entryDigest = posting.getCurrentRevision().getDigest();
        }
        if (sheriffOrderDetails.getCommentId() != null) {
            comment = commentRepository.findFullByNodeIdAndId(
                    requestContext.nodeId(), sheriffOrderDetails.getCommentId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
            if (!comment.getPosting().getId().equals(sheriffOrderDetails.getPostingId())) {
                throw new ObjectNotFoundFailure("comment.wrong-posting");
            }
            entryDigest = comment.getCurrentRevision().getDigest();
        }

        byte[] signingKey = namingCache.get(sheriffOrderDetails.getSheriffName()).getSigningKey();
        byte[] fingerprint = SheriffOrderFingerprintBuilder.build(
            sheriffOrderDetails.getSignatureVersion(), requestContext.nodeName(), sheriffOrderDetails, entryDigest
        );
        if (!CryptoUtil.verifySignature(fingerprint, sheriffOrderDetails.getSignature(), signingKey)) {
            throw new IncorrectSignatureException();
        }
        requestContext.authenticatedWithSignature(sheriffOrderDetails.getSheriffName());

        if (!feedOperations.isFeedSheriff(sheriffOrderDetails.getFeedName())) {
            throw new AuthenticationException();
        }

        if (posting == null) {
            String optionName = FeedOperations.getFeedSheriffMarksOption(sheriffOrderDetails.getFeedName());
            SheriffUtil.updateSheriffMarks(
                    sheriffOrderDetails.getSheriffName(),
                    sheriffOrderDetails.isDelete(),
                    () -> requestContext.getOptions().getString(optionName),
                    value -> requestContext.getOptions().set(optionName, value));
        } else {
            Entry entry = comment == null ? posting : comment;
            SheriffUtil.updateSheriffMarks(
                    sheriffOrderDetails.getSheriffName(), sheriffOrderDetails.isDelete(), entry);
            entry.setEditedAt(Util.now());
        }

        requestContext.send(new SheriffOrderReceivedLiberin(sheriffOrderDetails.isDelete(),
                sheriffOrderDetails.getFeedName(), posting, comment, sheriffOrderDetails.getSheriffName(),
                new AvatarImage(sheriffOrderDetails.getSheriffAvatar(), sheriffOrderDetails.getSheriffAvatarMediaFile()),
                sheriffOrderDetails.getId()));
        return Result.OK;
    }

}
