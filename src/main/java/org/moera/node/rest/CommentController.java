package org.moera.node.rest;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentText;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.CommentAddedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.naming.NamingCache;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.CommentOperations;
import org.moera.node.text.TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments")
public class CommentController {

    private static Logger log = LoggerFactory.getLogger(CommentController.class);

    private static final Duration CREATED_AT_MARGIN = Duration.ofMinutes(10);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private FingerprintManager fingerprintManager;

    @Inject
    private CommentOperations commentOperations;

    @Inject
    private TextConverter textConverter;

    @PostMapping
    @Transactional
    public ResponseEntity<CommentCreated> post(
            @PathVariable UUID postingId, @Valid @RequestBody CommentText commentText)
            throws AuthenticationException {

        log.info("POST /postings/{postingId}/comments (postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("comment.posting-not-found");
        }
        byte[] digest = validateCommentText(posting, commentText, commentText.getOwnerName());

        Comment comment = commentOperations.newComment(posting, commentText);
        try {
            comment = commentOperations.createOrUpdateComment(posting, comment, null, null,
                    revision -> commentText.toEntryRevision(revision, digest, textConverter));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("commentText.bodySrc.wrong-encoding");
        }

        requestContext.send(new CommentAddedEvent(comment));
        requestContext.send(new PostingCommentsChangedEvent(posting));
        requestContext.send(Directions.postingSubscribers(posting.getId()),
                new PostingCommentsUpdatedNotification(posting.getId(), posting.getChildrenTotal()));

        return ResponseEntity.created(URI.create("/postings/" + posting.getId() + "/comments" + comment.getId()))
                .body(new CommentCreated(comment, posting.getChildrenTotal()));
    }

    private byte[] validateCommentText(Posting posting, CommentText commentText, String ownerName)
            throws AuthenticationException {

        byte[] digest = null;
        if (commentText.getSignature() == null) {
            String clientName = requestContext.getClientName();
            if (StringUtils.isEmpty(clientName)) {
                throw new AuthenticationException();
            }
            if (!StringUtils.isEmpty(ownerName) && !ownerName.equals(clientName)) {
                throw new AuthenticationException();
            }
            commentText.setOwnerName(clientName);

            if (StringUtils.isEmpty(commentText.getBodySrc())) {
                throw new ValidationFailure("commentText.bodySrc.blank");
            }
        } else {
            byte[] signingKey = namingCache.get(ownerName).getSigningKey();
            Constructor<? extends Fingerprint> constructor = fingerprintManager.getConstructor(
                    FingerprintObjectType.COMMENT, commentText.getSignatureVersion(),
                    CommentText.class, byte[].class);
            if (!CryptoUtil.verify(
                    commentText.getSignature(),
                    signingKey,
                    constructor,
                    commentText,
                    posting.getCurrentRevision().getDigest())) {
                throw new IncorrectSignatureException();
            }
            digest = CryptoUtil.digest(constructor, commentText, posting.getCurrentRevision().getDigest());

            if (commentText.getBody() == null || StringUtils.isEmpty(commentText.getBody().getEncoded())) {
                throw new ValidationFailure("commentText.body.blank");
            }
            if (StringUtils.isEmpty(commentText.getBodyFormat())) {
                throw new ValidationFailure("commentText.bodyFormat.blank");
            }
            if (commentText.getCreatedAt() == null) {
                throw new ValidationFailure("commentText.createdAt.blank");
            }
            if (Duration.between(Instant.ofEpochSecond(commentText.getCreatedAt()), Instant.now()).abs()
                    .compareTo(CREATED_AT_MARGIN) > 0) {
                throw new ValidationFailure("commentText.createdAt.out-of-range");
            }
        }
        return digest;
    }

}
