package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.PostingOperations;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/deleted-postings")
@NoCache
public class DeletedPostingController {

    private static final Logger log = LoggerFactory.getLogger(DeletedPostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private EntryOperations entryOperations;

    @GetMapping
    @Admin(Scope.DELETE_OWN_CONTENT)
    @Transactional
    public List<PostingInfo> getAll(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer limit
    ) {
        log.info("GET /deleted-postings (page = {}, limit = {})", LogUtil.format(page), LogUtil.format(limit));

        page = page != null ? page : 0;
        ValidationUtil.assertion(page >= 0, "page.invalid");
        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");

        return postingRepository.findDeleted(requestContext.nodeId(),
            PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "deletedAt")))
            .stream()
            .map(p -> PostingInfoUtil.build(p, entryOperations, requestContext))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Admin(Scope.DELETE_OWN_CONTENT)
    @Transactional
    public PostingInfo get(@PathVariable UUID id) {
        log.info("GET /deleted-postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDeletedById(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));

        return PostingInfoUtil.build(posting, entryOperations, requestContext);
    }

    @PostMapping("/{id}/restore")
    @Admin(Scope.DELETE_OWN_CONTENT)
    @Entitled
    @Transactional
    public PostingInfo restore(@PathVariable UUID id) {
        log.info("POST /deleted-postings/{id}/restore (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDeletedWithAttachmentsById(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));

        posting.setDeletedAt(null);
        posting.setDeadline(null);
        List<MediaFileOwner> media = posting.getCurrentRevision().getAttachments().stream()
            .map(EntryAttachment::getMediaFileOwner)
            .collect(Collectors.toList());
        posting = postingOperations.createOrUpdatePosting(
            posting, posting.getCurrentRevision(), media, null, null, null, null
        );

        requestContext.send(new PostingRestoredLiberin(posting));

        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);
        return PostingInfoUtil.build(
            posting, stories, MediaAttachmentsProvider.RELATIONS, requestContext, requestContext.getOptions()
        );
    }

    @Scheduled(fixedDelayString = "P1D")
    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired deleted postings");

            postingRepository.deleteExpired(Util.now());
        }
    }

}
