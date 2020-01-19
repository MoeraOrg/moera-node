package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.TimelineInfo;
import org.moera.node.model.TimelineSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/timeline")
public class TimelineController {

    private static Logger log = LoggerFactory.getLogger(TimelineController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @GetMapping
    public TimelineInfo get() {
        log.info("GET /timeline");

        return new TimelineInfo();
    }

    @GetMapping("/postings")
    public TimelineSliceInfo getPostings(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /timeline/postings (before = {}, after = {}, limit = {})",
                LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        if (before != null && after != null) {
            throw new ValidationFailure("timeline.before-after-exclusive");
        }

        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        if (after == null) {
            before = before != null ? before : Long.MAX_VALUE;
            return getPostingsBefore(before, limit);
        } else {
            return getPostingsAfter(after, limit);
        }
    }

    private TimelineSliceInfo getPostingsBefore(long before, int limit) {
        Page<Posting> page = postingRepository.findSlice(requestContext.nodeId(), Long.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "currentRevision.moment"));
        TimelineSliceInfo sliceInfo = new TimelineSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getCurrentRevision().getMoment());
        }
        fillSlice(sliceInfo, limit);
        return sliceInfo;
    }

    private TimelineSliceInfo getPostingsAfter(long after, int limit) {
        Page<Posting> page = postingRepository.findSlice(requestContext.nodeId(), after, Long.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "currentRevision.moment"));
        TimelineSliceInfo sliceInfo = new TimelineSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(Long.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getCurrentRevision().getMoment());
        }
        fillSlice(sliceInfo, limit);
        return sliceInfo;
    }

    private void fillSlice(TimelineSliceInfo sliceInfo, int limit) {
        List<PostingInfo> postings = postingRepository.findInRange(
                requestContext.nodeId(), sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .map(PostingInfo::new)
                .sorted(Comparator.comparingLong(PostingInfo::getMoment).reversed())
                .collect(Collectors.toList());
        String clientName = requestContext.getClientName();
        if (!StringUtils.isEmpty(clientName)) {
            Map<String, PostingInfo> postingMap = postings.stream().collect(
                    Collectors.toMap(PostingInfo::getId, Function.identity()));
            reactionRepository.findByEntriesInRangeAndOwner(
                    requestContext.nodeId(), sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                    .stream()
                    .map(ClientReactionInfo::new)
                    .filter(r -> postingMap.containsKey(r.getPostingId()))
                    .forEach(r -> postingMap.get(r.getPostingId()).setClientReaction(r));
        }
        if (postings.size() > limit) {
            postings.remove(limit);
        }
        sliceInfo.setPostings(postings);
    }

}
