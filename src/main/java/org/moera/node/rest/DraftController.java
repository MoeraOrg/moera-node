package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.DraftType;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.DraftAddedLiberin;
import org.moera.node.liberin.model.DraftDeletedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.DraftInfo;
import org.moera.node.model.DraftText;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.RemoteMedia;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.body.Body;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.operations.PostingOperations;
import org.moera.node.text.MediaExtractor;
import org.moera.node.text.TextConverter;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/drafts")
@NoCache
public class DraftController {

    private static final Logger log = LoggerFactory.getLogger(DraftController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private DraftRepository draftRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private TextConverter textConverter;

    @Inject
    private MediaOperations mediaOperations;

    @GetMapping
    @Admin
    @Transactional
    public List<DraftInfo> getAll(
            @RequestParam DraftType draftType,
            @RequestParam String nodeName,
            @RequestParam(required = false) String postingId,
            @RequestParam(required = false) String commentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) throws MissingServletRequestParameterException {

        log.info("GET /drafts (draftType = {}, nodeName = {}, postingId = {}, commentId = {}, page = {}, limit = {})",
                LogUtil.format(draftType.toString()), LogUtil.format(nodeName), LogUtil.format(postingId),
                LogUtil.format(commentId), LogUtil.format(page), LogUtil.format(limit));

        if ((draftType == DraftType.POSTING_UPDATE || draftType == DraftType.NEW_COMMENT
                || draftType == DraftType.COMMENT_UPDATE) && ObjectUtils.isEmpty(postingId)) {
            throw new MissingServletRequestParameterException("postingId", "string");
        }
        if (draftType == DraftType.COMMENT_UPDATE && ObjectUtils.isEmpty(commentId)) {
            throw new MissingServletRequestParameterException("commentId", "string");
        }

        page = page != null ? page : 0;
        if (page < 0) {
            throw new ValidationFailure("page.invalid");
        }
        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Draft> drafts;
        switch (draftType) {
            case NEW_POSTING:
                drafts = draftRepository.findAllNewPosting(
                        requestContext.nodeId(), requestContext.nodeName(), pageable);
                break;

            case POSTING_UPDATE: {
                drafts = draftRepository.findPostingUpdate(
                        requestContext.nodeId(), requestContext.nodeName(), postingId, pageable);
                break;
            }

            case NEW_COMMENT:
                drafts = draftRepository.findAllNewComment(
                        requestContext.nodeId(), nodeName, postingId, pageable);
                break;

            case COMMENT_UPDATE: {
                drafts = draftRepository.findCommentUpdate(
                        requestContext.nodeId(), nodeName, postingId, commentId, pageable);
                break;
            }

            default:
                drafts = Collections.emptyList();
                break;
        }
        return drafts.stream()
                .map(DraftInfo::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public ResponseEntity<DraftInfo> post(@Valid @RequestBody DraftText draftText) {
        log.info("POST /drafts (draftType = {}, nodeName = {}, postingId = {}, commentId = {}, bodySrc = {},"
                        + " bodySrcFormat = {})",
                LogUtil.format(draftText.getDraftType().toString()), LogUtil.format(draftText.getReceiverName()),
                LogUtil.format(draftText.getReceiverPostingId()), LogUtil.format(draftText.getReceiverCommentId()),
                LogUtil.format(draftText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(draftText.getBodySrcFormat())));

        if ((draftText.getDraftType() == DraftType.POSTING_UPDATE
                || draftText.getDraftType() == DraftType.NEW_COMMENT
                || draftText.getDraftType() == DraftType.COMMENT_UPDATE)
                && ObjectUtils.isEmpty(draftText.getReceiverPostingId())) {
            throw new ValidationFailure("draftText.postingId.blank");
        }
        if (draftText.getDraftType() == DraftType.COMMENT_UPDATE
                && ObjectUtils.isEmpty(draftText.getReceiverCommentId())) {
            throw new ValidationFailure("draftText.commentId.blank");
        }

        List<MediaFileOwner> media = validate(draftText);

        Draft draft = new Draft();
        draft.setId(UUID.randomUUID());
        draft.setNodeId(requestContext.nodeId());
        draft.setReceiverName(draftText.getReceiverName());
        draft.setReceiverPostingId(draftText.getReceiverPostingId());
        draft.setReceiverCommentId(draftText.getReceiverCommentId());
        draft.setOwnerFullName(requestContext.fullName());
        if (requestContext.getAvatar() != null) {
            draft.setOwnerAvatarMediaFile(requestContext.getAvatar().getMediaFile());
            draft.setOwnerAvatarShape(requestContext.getAvatar().getShape());
        }
        draft.setCreatedAt(Util.now());
        try {
            draftText.toDraft(draft, textConverter);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("draftText.bodySrc.wrong-encoding");
        }
        updateDeadline(draft);
        draft = draftRepository.save(draft);
        updateAttachments(draft, media, draftText.getMedia());

        requestContext.send(new DraftAddedLiberin(draft));

        return ResponseEntity.created(URI.create("/drafts/" + draft.getId())).body(new DraftInfo(draft));
    }

    @PutMapping("/{id}")
    @Admin
    @Entitled
    @Transactional
    public DraftInfo put(@PathVariable UUID id, @Valid @RequestBody DraftText draftText) {
        log.info("PUT /drafts/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(id),
                LogUtil.format(draftText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(draftText.getBodySrcFormat())));

        List<MediaFileOwner> media = validate(draftText);

        Draft draft = draftRepository.findById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("draft.not-found"));
        try {
            draftText.toDraft(draft, textConverter);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("draftText.bodySrc.wrong-encoding");
        }
        updateDeadline(draft);
        updateAttachments(draft, media, draftText.getMedia());

        requestContext.send(new DraftUpdatedLiberin(draft));

        return new DraftInfo(draft);
    }

    private List<MediaFileOwner> validate(@RequestBody @Valid DraftText draftText) {
        if (draftText.getBodySrc() != null && draftText.getBodySrc().length() > getMaxPostingSize()) {
            throw new ValidationFailure("draftText.bodySrc.wrong-size");
        }

        if (draftText.getOwnerAvatar() != null && draftText.getOwnerAvatar().getMediaId() != null) {
            MediaFile mediaFile = mediaFileRepository.findById(draftText.getOwnerAvatar().getMediaId()).orElse(null);
            if (mediaFile == null || !mediaFile.isExposed()) {
                throw new ValidationFailure("draftText.ownerAvatar.mediaId.not-found");
            }
            draftText.setOwnerAvatarMediaFile(mediaFile);
        }

        if (draftText.getReceiverName().equals(requestContext.nodeName())) {
            if (draftText.getMedia() == null) {
                return Collections.emptyList();
            }

            UUID[] ids;
            try {
                ids = Arrays.stream(draftText.getMedia())
                        .map(RemoteMedia::getId)
                        .map(UUID::fromString)
                        .toArray(UUID[]::new);
            } catch (IllegalArgumentException e) {
                throw new ValidationFailure("draftText.media.not-found");
            }

            return mediaOperations.validateAttachments(ids,
                    () -> new ValidationFailure("draftText.media.not-found"),
                    null, requestContext.isAdmin(), requestContext.getClientName());
        } else {
            return Collections.emptyList();
        }
    }

    private void updateAttachments(Draft draft, List<MediaFileOwner> media, RemoteMedia[] remoteMedia) {
        Set<EntryAttachment> attachments = new HashSet<>(draft.getAttachments());
        for (EntryAttachment ea : attachments) {
            draft.removeAttachment(ea);
            entryAttachmentRepository.delete(ea);
        }

        Set<String> embedded = MediaExtractor.extractMediaFileIds(new Body(draft.getBody()));

        int ordinal = 0;
        if (draft.getReceiverName().equals(requestContext.nodeName())) {
            for (MediaFileOwner mfo : media) {
                EntryAttachment attachment = new EntryAttachment(draft, mfo, ordinal++);
                attachment.setEmbedded(embedded.contains(mfo.getMediaFile().getId()));
                attachment = entryAttachmentRepository.save(attachment);
                draft.addAttachment(attachment);
            }
        } else {
            for (RemoteMedia md : remoteMedia) {
                EntryAttachment attachment = new EntryAttachment(draft, md, ordinal++);
                attachment.setEmbedded(embedded.contains(md.getHash()));
                attachment = entryAttachmentRepository.save(attachment);
                draft.addAttachment(attachment);
            }
        }
    }

    private int getMaxPostingSize() {
        return requestContext.getOptions().getInt("posting.max-size");
    }

    private void updateDeadline(Draft draft) {
        String ttlOptionName = draft.getDraftType() == DraftType.NEW_POSTING
                || draft.getDraftType() == DraftType.POSTING_UPDATE
                ? "posting.draft.lifetime" : "comment.draft.lifetime";
        ExtendedDuration draftTtl = requestContext.getOptions().getDuration(ttlOptionName);
        if (!draftTtl.isNever()) {
            draft.setDeadline(Timestamp.from(Instant.now().plus(draftTtl.getDuration())));
        }
    }

    @GetMapping("/{id}")
    @Admin
    @Transactional
    public DraftInfo get(@PathVariable UUID id) {
        log.info("GET /drafts/{id}, (id = {})", LogUtil.format(id));

        Draft draft = draftRepository.findById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("draft.not-found"));

        return new DraftInfo(draft);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /drafts/{id}, (id = {})", LogUtil.format(id));

        Draft draft = draftRepository.findById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("draft.not-found"));
        draftRepository.delete(draft);

        requestContext.send(new DraftDeletedLiberin(draft));

        return Result.OK;
    }

    @Scheduled(fixedDelayString = "P1D")
    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void purgeExpired() {
        try {
            draftRepository.deleteExpired(Util.now());
        } catch (Exception e) {
            log.error("Error purging expired drafts", e);
        }
    }

}
