package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

}
