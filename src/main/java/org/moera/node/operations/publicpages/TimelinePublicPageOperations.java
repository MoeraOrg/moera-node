package org.moera.node.operations.publicpages;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.Feed;
import org.moera.node.data.PublicPage;
import org.moera.node.data.StoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TimelinePublicPageOperations extends PublicPageOperations {

    @Inject
    private StoryRepository storyRepository;

    public TimelinePublicPageOperations() {
        super(30, 20);
    }

    public void updatePublicPages(long moment) {
        super.updatePublicPages(null, moment);
    }

    @Override
    protected Entry findEntryById(UUID entryId) {
        return null;
    }

    @Override
    protected PublicPage findContaining(UUID entryId, long moment) {
        return publicPageRepository.findContaining(universalContext.nodeId(), moment);
    }

    @Override
    protected int countInRange(UUID entryId, long after, long before) {
        return storyRepository.countInRange(universalContext.nodeId(), Feed.TIMELINE, after, before);
    }

    @Override
    protected Page<Long> findMomentsInRange(UUID entryId, long after, long before, Pageable pageable) {
        return storyRepository.findMomentsInRange(universalContext.nodeId(), Feed.TIMELINE, after, before, pageable);
    }

    @Override
    protected int countNumber(UUID entryId, long moment) {
        return publicPageRepository.countNumber(universalContext.nodeId(), moment);
    }

    @Override
    protected int countTotal(UUID entryId) {
        return publicPageRepository.countTotal(universalContext.nodeId());
    }

    @Override
    protected Page<PublicPage> findPages(UUID entryId, Long moment, int page, int size) {
        moment = moment != null ? moment : Long.MAX_VALUE;
        return publicPageRepository.findAllBeforeMoment(
            universalContext.nodeId(), moment, PageRequest.of(page, size, Sort.Direction.DESC, "beforeMoment")
        );
    }

}
