package org.moera.node.rest;

import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.TimelineSliceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/timeline")
public class TimelineController {

    private static final int MAX_POSTINGS_PER_REQUEST = 200;

    private static Logger log = LoggerFactory.getLogger(TimelineController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @GetMapping("/postings")
    @ResponseBody
    public TimelineSliceInfo getPostings(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        before = before != null ? before : Long.MAX_VALUE;
        after = after != null ? after : 0;
        limit = limit != null && limit <= MAX_POSTINGS_PER_REQUEST ? limit : MAX_POSTINGS_PER_REQUEST;

        Page<Posting> page = postingRepository.findSlice(requestContext.nodeId(), after, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        TimelineSliceInfo sliceInfo = new TimelineSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(after);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        sliceInfo.setPostings(page.stream().map(PostingInfo::new).collect(Collectors.toList()));
        if (sliceInfo.getPostings().size() > limit) {
            sliceInfo.getPostings().remove(limit.intValue()); // To call remove(int) instead of remove(Object)
        }
        return sliceInfo;
    }

}
