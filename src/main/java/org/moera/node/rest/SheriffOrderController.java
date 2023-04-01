package org.moera.node.rest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SheriffMark;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.CommentUpdatedLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffOrderDetails;
import org.moera.node.model.ValidationFailure;
import org.moera.node.naming.NamingCache;
import org.moera.node.operations.FeedOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
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
    private NamingCache namingCache;

    @Inject
    private ObjectMapper objectMapper;

    @PostMapping
    @Transactional
    public Result post(@Valid @RequestBody SheriffOrderDetails sheriffOrderDetails) throws JsonProcessingException {
        log.info("POST /sheriff/orders"
                    + " (delete = {}, sheriffName = {}, postingId = {}, commentId = {}, category = {}, reasonCode = {})",
                LogUtil.format(sheriffOrderDetails.isDelete()),
                LogUtil.format(sheriffOrderDetails.getSheriffName()),
                LogUtil.format(sheriffOrderDetails.getPostingId()),
                LogUtil.format(sheriffOrderDetails.getCommentId()),
                LogUtil.format(sheriffOrderDetails.getCategory().getValue()),
                LogUtil.format(sheriffOrderDetails.getReasonCode().getValue()));

        if (Duration.between(Instant.ofEpochSecond(sheriffOrderDetails.getCreatedAt()), Instant.now()).abs()
                .compareTo(CREATED_AT_MARGIN) > 0) {
            throw new ValidationFailure("sheriffOrderDetails.createdAt.out-of-range");
        }

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
        Fingerprint fingerprint = Fingerprints.sheriffOrder(sheriffOrderDetails.getSignatureVersion())
                .create(requestContext.nodeName(), sheriffOrderDetails, entryDigest);
        if (!CryptoUtil.verify(fingerprint, sheriffOrderDetails.getSignature(), signingKey)) {
            throw new IncorrectSignatureException();
        }
        requestContext.authenticatedWithSignature(sheriffOrderDetails.getSheriffName());

        if (!feedOperations.isFeedSheriff(sheriffOrderDetails.getFeedName())) {
            throw new AuthenticationException();
        }

        if (posting == null) {
            String optionName = String.format("sheriffs.%s.marks", sheriffOrderDetails.getFeedName());
            updateSheriffMarks(sheriffOrderDetails,
                    () -> requestContext.getOptions().getString(optionName),
                    value -> requestContext.getOptions().set(optionName, value));
        } else {
            Entry entry = comment == null ? posting : comment;
            updateSheriffMarks(sheriffOrderDetails, entry::getSheriffMarks, entry::setSheriffMarks);

            requestContext.send(comment == null
                    ? new PostingUpdatedLiberin(posting, posting.getCurrentRevision(), posting.getViewE())
                    : new CommentUpdatedLiberin(comment, comment.getCurrentRevision(), comment.getViewE()));
        }

        return Result.OK;
    }

    private void updateSheriffMarks(SheriffOrderDetails details,
                                    Supplier<String> getter, Consumer<String> setter) throws JsonProcessingException {
        String marksS = getter.get();
        List<SheriffMark> marks;
        if (!ObjectUtils.isEmpty(marksS)) {
            marks = Arrays.stream(objectMapper.readValue(marksS, SheriffMark[].class))
                    .filter(mark -> !mark.getSheriffName().equals(details.getSheriffName()))
                    .collect(Collectors.toList());
        } else {
            marks = new ArrayList<>();
        }
        if (!details.isDelete()) {
            marks.add(new SheriffMark(details.getSheriffName()));
        }
        setter.accept(!marks.isEmpty() ? objectMapper.writeValueAsString(marks) : "");
    }

}
