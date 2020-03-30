package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.event.model.PostingRestoredEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ValidationFailure;
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
public class DeletedPostingController {

    private static Logger log = LoggerFactory.getLogger(DeletedPostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PostingOperations postingOperations;

    @GetMapping
    @Admin
    public List<PostingInfo> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /deleted-postings (page = {}, limit = {})", LogUtil.format(page), LogUtil.format(limit));

        page = page != null ? page : 0;
        if (page < 0) {
            throw new ValidationFailure("page.invalid");
        }
        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        return postingRepository.findDeleted(requestContext.nodeId(),
                PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "deletedAt")))
                .stream()
                .map(p -> new PostingInfo(p, true))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Admin
    public PostingInfo get(@PathVariable UUID id) {
        log.info("GET /deleted-postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDeletedById(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return new PostingInfo(posting, true);
    }

    @PostMapping("/{id}/restore")
    @Admin
    @Entitled
    @Transactional
    public PostingInfo restore(@PathVariable UUID id) {
        log.info("POST /deleted-postings/{id}/restore (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findDeletedById(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        posting.setDeletedAt(null);
        posting.setDeadline(null);
        posting = postingOperations.createOrUpdatePosting(posting, posting.getCurrentRevision(), null, null);
        requestContext.send(new PostingRestoredEvent(posting));

        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);
        return new PostingInfo(posting, stories, true);
    }

    @Scheduled(fixedDelayString = "P1D")
    @EventListener(DomainsConfiguredEvent.class)
    @Transactional
    public void purgeExpired() {
        postingRepository.deleteExpired(Util.now());
    }

}
