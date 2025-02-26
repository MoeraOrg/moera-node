package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.DraftInfo;
import org.moera.lib.node.types.DraftText;
import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.body.BodyMappingException;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Draft;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.model.DraftAddedLiberin;
import org.moera.node.liberin.model.DraftDeletedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.DraftInfoUtil;
import org.moera.node.model.DraftTextUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ValidationFailure;
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
    private RequestCounter requestCounter;

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
    @Admin(Scope.DRAFTS)
    @Transactional
    public List<DraftInfo> getAll(
        @RequestParam DraftType draftType,
        @RequestParam String nodeName,
        @RequestParam(required = false) String postingId,
        @RequestParam(required = false) String commentId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer limit
    ) throws MissingServletRequestParameterException {
        log.info(
            "GET /drafts (draftType = {}, nodeName = {}, postingId = {}, commentId = {}, page = {}, limit = {})",
            LogUtil.format(draftType.toString()), LogUtil.format(nodeName), LogUtil.format(postingId),
            LogUtil.format(commentId), LogUtil.format(page), LogUtil.format(limit)
        );

        if (
            (
                draftType == DraftType.POSTING_UPDATE
                || draftType == DraftType.NEW_COMMENT
                || draftType == DraftType.COMMENT_UPDATE
            )
            && ObjectUtils.isEmpty(postingId)
        ) {
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
            ? limit
            : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Draft> drafts;
        switch (draftType) {
            case NEW_POSTING:
                drafts = draftRepository.findAllNewPosting(
                    requestContext.nodeId(), requestContext.nodeName(), pageable
                );
                break;

            case POSTING_UPDATE: {
                drafts = draftRepository.findPostingUpdate(
                    requestContext.nodeId(), requestContext.nodeName(), postingId, pageable
                );
                break;
            }

            case NEW_COMMENT:
                drafts = draftRepository.findAllNewComment(
                    requestContext.nodeId(), nodeName, postingId, pageable
                );
                break;

            case COMMENT_UPDATE: {
                drafts = draftRepository.findCommentUpdate(
                    requestContext.nodeId(), nodeName, postingId, commentId, pageable
                );
                break;
            }

            default:
                drafts = Collections.emptyList();
                break;
        }
        return drafts.stream()
            .map(DraftInfoUtil::build)
            .collect(Collectors.toList());
    }

    @PostMapping
    @Admin(Scope.DRAFTS)
    @Entitled
    @Transactional
    public ResponseEntity<DraftInfo> post(@RequestBody DraftText draftText) {
        log.info(
            "POST /drafts (draftType = {}, nodeName = {}, postingId = {}, commentId = {}, bodySrc = {},"
                + " bodySrcFormat = {})",
            LogUtil.format(draftText.getDraftType().toString()), LogUtil.format(draftText.getReceiverName()),
            LogUtil.format(draftText.getReceiverPostingId()), LogUtil.format(draftText.getReceiverCommentId()),
            LogUtil.format(draftText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(draftText.getBodySrcFormat()))
        );

        draftText.validate();

        ValidationUtil.assertion(
            draftText.getDraftType() == DraftType.NEW_POSTING
                || !ObjectUtils.isEmpty(draftText.getReceiverPostingId()),
            "draft.receiver-posting-id.blank"
        );
        ValidationUtil.assertion(
            draftText.getDraftType() != DraftType.COMMENT_UPDATE
                || !ObjectUtils.isEmpty(draftText.getReceiverCommentId()),
            "draft.receiver-comment-id.blank"
        );

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
            DraftTextUtil.toDraft(draftText, draft, textConverter);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("draft.body-src.wrong-encoding");
        }
        updateDeadline(draft);
        draft = draftRepository.save(draft);
        updateAttachments(draft, media, draftText.getMedia());

        requestContext.send(new DraftAddedLiberin(draft));

        return ResponseEntity.created(URI.create("/drafts/" + draft.getId())).body(DraftInfoUtil.build(draft));
    }

    @PutMapping("/{id}")
    @Admin(Scope.DRAFTS)
    @Entitled
    @Transactional
    public DraftInfo put(@PathVariable UUID id, @RequestBody DraftText draftText) {
        log.info(
            "PUT /drafts/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(id),
            LogUtil.format(draftText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(draftText.getBodySrcFormat()))
        );

        draftText.validate();
        List<MediaFileOwner> media = validate(draftText);

        Draft draft = draftRepository.findById(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("draft.not-found"));
        try {
            DraftTextUtil.toDraft(draftText, draft, textConverter);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("draft.body-src.wrong-encoding");
        }
        updateDeadline(draft);
        updateAttachments(draft, media, draftText.getMedia());

        requestContext.send(new DraftUpdatedLiberin(draft));

        return DraftInfoUtil.build(draft);
    }

    private List<MediaFileOwner> validate(DraftText draftText) {
        if (draftText.getBodySrc() != null && draftText.getBodySrc().getEncoded().length() > getMaxPostingSize()) {
            throw new ValidationFailure("draft.body-src.wrong-size");
        }

        if (draftText.getOwnerAvatar() != null && draftText.getOwnerAvatar().getMediaId() != null) {
            MediaFile mediaFile = mediaFileRepository.findById(draftText.getOwnerAvatar().getMediaId()).orElse(null);
            if (mediaFile == null || !mediaFile.isExposed()) {
                throw new ObjectNotFoundFailure("avatar.not-found");
            }
            AvatarDescriptionUtil.setMediaFile(draftText.getOwnerAvatar(), mediaFile);
        }

        if (draftText.getReceiverName().equals(requestContext.nodeName())) {
            return mediaOperations.validateAttachments(
                draftText.getMedia(),
                RemoteMedia::getId,
                false,
                requestContext.isAdmin(Scope.VIEW_MEDIA),
                requestContext.isAdmin(Scope.DRAFTS),
                requestContext.getClientName(Scope.VIEW_MEDIA)
            );
        } else {
            return Collections.emptyList();
        }
    }

    private void updateAttachments(Draft draft, List<MediaFileOwner> media, List<RemoteMedia> remoteMedia) {
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
        String ttlOptionName =
            draft.getDraftType() == DraftType.NEW_POSTING || draft.getDraftType() == DraftType.POSTING_UPDATE
                ? "posting.draft.lifetime"
                : "comment.draft.lifetime";
        ExtendedDuration draftTtl = requestContext.getOptions().getDuration(ttlOptionName);
        if (!draftTtl.isNever()) {
            draft.setDeadline(Timestamp.from(Instant.now().plus(draftTtl.getDuration())));
        }
    }

    @GetMapping("/{id}")
    @Admin(Scope.DRAFTS)
    @Transactional
    public DraftInfo get(@PathVariable UUID id) {
        log.info("GET /drafts/{id}, (id = {})", LogUtil.format(id));

        Draft draft = draftRepository.findById(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("draft.not-found"));

        return DraftInfoUtil.build(draft);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.DRAFTS)
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
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired drafts");

            draftRepository.deleteExpired(Util.now());
        }
    }

}
