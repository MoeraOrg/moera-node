package org.moera.node.rest;

import java.util.Comparator;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.TimelineInfo;
import org.moera.node.model.TimelineSliceInfo;
import org.moera.node.model.ValidationFailure;
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

    @GetMapping
    @ResponseBody
    public TimelineInfo get() {
        log.info("GET /timeline");

        return new TimelineInfo();
    }

    @GetMapping("/postings")
    @ResponseBody
    public TimelineSliceInfo getPostings(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /timeline/postings (before = {}, after = {}, limit = {})",
                LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        if (before != null && after != null) {
            throw new ValidationFailure("timeline.before-after-exclusive");
        }

        limit = limit != null && limit <= MAX_POSTINGS_PER_REQUEST ? limit : MAX_POSTINGS_PER_REQUEST;
        if (after == null) {
            before = before != null ? before : Long.MAX_VALUE;
            return getPostingsBefore(before, limit);
        } else {
            return getPostingsAfter(after, limit);
        }
    }

    private TimelineSliceInfo getPostingsBefore(long before, int limit) {
        Page<Posting> page = postingRepository.findSlice(requestContext.nodeId(), Long.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        TimelineSliceInfo sliceInfo = new TimelineSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        sliceInfo.setPostings(page.stream().map(PostingInfo::new).collect(Collectors.toList()));
        if (sliceInfo.getPostings().size() > limit) {
            sliceInfo.getPostings().remove(limit);
        }
        return sliceInfo;
    }

    private TimelineSliceInfo getPostingsAfter(long after, int limit) {
        Page<Posting> page = postingRepository.findSlice(requestContext.nodeId(), after, Long.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "moment"));
        TimelineSliceInfo sliceInfo = new TimelineSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(Long.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        sliceInfo.setPostings(page.stream().map(PostingInfo::new).collect(Collectors.toList()));
        if (sliceInfo.getPostings().size() > limit) {
            sliceInfo.getPostings().remove(limit);
        }
        sliceInfo.getPostings().sort(Comparator.comparingLong(PostingInfo::getMoment).reversed());
        return sliceInfo;
    }

}
