package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/revisions")
public class PostingRevisionController {

    private static Logger log = LoggerFactory.getLogger(PostingRevisionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PostingOperations postingOperations;

    @GetMapping
    @ResponseBody
    public List<PostingRevisionInfo> getAll(@PathVariable UUID postingId) {
        log.info("GET /postings/{postingId}/revisions (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return posting.getRevisions().stream()
                .map(PostingRevisionInfo::new)
                .sorted(Comparator.comparing(PostingRevisionInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ResponseBody
    public PostingRevisionInfo get(@PathVariable UUID postingId, @PathVariable UUID id) {
        log.info("GET /postings/{postingId}/revisions/{id} (postingId = {}, id = {})",
                LogUtil.format(postingId),
                LogUtil.format(id));

        PostingRevisionInfo info = entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), postingId, id)
                .map(PostingRevisionInfo::new).orElse(null);
        if (info == null) {
            throw new ObjectNotFoundFailure("posting-revision.not-found");
        }

        return info;
    }

    @PostMapping("/{id}/restore")
    @Admin
    @ResponseBody
    @Transactional
    public PostingRevisionInfo restore(@PathVariable UUID postingId, @PathVariable UUID id) {
        log.info("POST /postings/{postingId}/revisions/{id}/restore (postingId = {}, id = {})",
                LogUtil.format(postingId),
                LogUtil.format(id));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (posting.getCurrentRevision().getId().equals(id)) {
            throw new ValidationFailure("posting-revision.already-current");
        }
        EntryRevision revision = entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), postingId, id)
                .orElse(null);
        if (revision == null) {
            throw new ObjectNotFoundFailure("posting-revision.not-found");
        }

        postingOperations.createOrUpdatePosting(posting, revision, null);

        return new PostingRevisionInfo(revision);
    }

}
