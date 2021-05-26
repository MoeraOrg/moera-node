package org.moera.node.rest;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.DraftPostingAddedEvent;
import org.moera.node.model.event.DraftPostingDeletedEvent;
import org.moera.node.model.event.DraftPostingUpdatedEvent;
import org.moera.node.operations.PostingOperations;
import org.moera.node.text.TextConverter;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/draft-postings")
@NoCache
public class DraftPostingController {

    private static Logger log = LoggerFactory.getLogger(DraftPostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private TextConverter textConverter;

    @GetMapping
    @Admin
    public List<PostingInfo> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /draft-postings (page = {}, limit = {})", LogUtil.format(page), LogUtil.format(limit));

        page = page != null ? page : 0;
        if (page < 0) {
            throw new ValidationFailure("page.invalid");
        }
        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        return postingRepository.findDrafts(requestContext.nodeId(),
                PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(p -> new PostingInfo(p, p.getDraftRevision(), true, true))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public ResponseEntity<PostingInfo> post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /draft-postings (bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        if (postingText.getOwnerAvatar() != null && postingText.getOwnerAvatar().getMediaId() != null) {
            MediaFile mediaFile = mediaFileRepository.findById(postingText.getOwnerAvatar().getMediaId()).orElse(null);
            if (mediaFile == null || !mediaFile.isExposed()) {
                throw new ValidationFailure("postingText.ownerAvatar.mediaId.not-found");
            }
            postingText.setOwnerAvatarMediaFile(mediaFile);
        }

        Posting posting = postingOperations.newPosting(postingText, p -> {
            p.setDraft(true);
            ExtendedDuration draftTtl = requestContext.getOptions().getDuration("posting.draft.lifetime");
            if (!draftTtl.isNever()) {
                p.setDeadline(Timestamp.from(Instant.now().plus(draftTtl.getDuration())));
            }
        });
        try {
            posting = postingOperations.createOrUpdatePostingDraft(posting, null, null,
                    revision -> postingText.toEntryRevision(revision, textConverter));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        requestContext.send(new DraftPostingAddedEvent(posting));

        return ResponseEntity.created(
                URI.create("/draft-postings/" + posting.getId()))
                    .body(new PostingInfo(posting, posting.getDraftRevision(), true, true));
    }

    @PutMapping("/{id}")
    @Admin
    @Entitled
    @Transactional
    public PostingInfo put(@PathVariable UUID id, @Valid @RequestBody PostingText postingText) {
        log.info("PUT /draft-postings/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(id),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        if (postingText.getOwnerAvatar() != null && postingText.getOwnerAvatar().getMediaId() != null) {
            MediaFile mediaFile = mediaFileRepository.findById(postingText.getOwnerAvatar().getMediaId()).orElse(null);
            if (mediaFile == null || !mediaFile.isExposed()) {
                throw new ValidationFailure("postingText.ownerAvatar.mediaId.not-found");
            }
            postingText.setOwnerAvatarMediaFile(mediaFile);
        }

        Posting posting = postingRepository.findDraftById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        postingText.toEntry(posting);
        ExtendedDuration draftTtl = requestContext.getOptions().getDuration("posting.draft.lifetime");
        if (!draftTtl.isNever()) {
            posting.setDeadline(Timestamp.from(Instant.now().plus(draftTtl.getDuration())));
        }
        try {
            posting = postingOperations.createOrUpdatePostingDraft(posting, posting.getDraftRevision(), null,
                    revision -> postingText.toEntryRevision(revision, textConverter));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        requestContext.send(new DraftPostingUpdatedEvent(posting));

        return new PostingInfo(posting, posting.getDraftRevision(), true, true);
    }

    @GetMapping("/{id}")
    @Admin
    public PostingInfo get(@PathVariable UUID id) {
        log.info("GET /draft-postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDraftById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));

        return new PostingInfo(posting, posting.getDraftRevision(), true,
                requestContext.isAdmin() || requestContext.isClient(posting.getOwnerName()));
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /draft-postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDraftById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        posting.setDeletedAt(Util.now());
        ExtendedDuration postingTtl = requestContext.getOptions().getDuration("posting.deleted.lifetime");
        if (!postingTtl.isNever()) {
            posting.setDeadline(Timestamp.from(Instant.now().plus(postingTtl.getDuration())));
        }
        posting.getDraftRevision().setDeletedAt(Util.now());
        entryRevisionRepository.save(posting.getDraftRevision());

        requestContext.send(new DraftPostingDeletedEvent(posting));

        return Result.OK;
    }

}
