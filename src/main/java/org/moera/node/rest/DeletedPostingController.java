package org.moera.node.rest;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/deleted-postings")
public class DeletedPostingController {

    private static Logger log = LoggerFactory.getLogger(DeletedPostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @GetMapping
    @Admin
    @ResponseBody
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
                .map(PostingInfo::new)
                .collect(Collectors.toList());
    }

}
