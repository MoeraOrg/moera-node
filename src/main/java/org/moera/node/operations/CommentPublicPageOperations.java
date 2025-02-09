package org.moera.node.operations;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CommentPublicPageOperations extends PublicPageOperations {

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostingRepository postingRepository;

    public CommentPublicPageOperations() {
        super(60, 40);
    }

    public void updatePublicPages(UUID postingId, long moment) {
        super.updatePublicPages(postingId, moment);
    }

    @Override
    protected Entry findEntryById(UUID entryId) {
        return postingRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("entryId should exist"));
    }

    @Override
    protected PublicPage findByBeforeMoment(UUID entryId, long before) {
        return publicPageRepository.findByBeforeMomentForEntry(requestContext.nodeId(), entryId, before);
    }

    @Override
    protected PublicPage findByAfterMoment(UUID entryId, long after) {
        return publicPageRepository.findByAfterMomentForEntry(requestContext.nodeId(), entryId, after);
    }

    @Override
    protected int countInRange(UUID entryId, long after, long before) {
        return commentRepository.countInRange(requestContext.nodeId(), entryId, after, before);
    }

    @Override
    protected Page<Long> findMomentsInRange(UUID entryId, long after, long before, Pageable pageable) {
        return commentRepository.findMomentsInRange(requestContext.nodeId(), entryId, after, before, pageable);
    }

    @Override
    protected int countNumber(UUID entryId, long moment) {
        return publicPageRepository.countNumberForEntry(requestContext.nodeId(), entryId, moment);
    }

    @Override
    protected int countTotal(UUID entryId) {
        return publicPageRepository.countTotalForEntry(requestContext.nodeId(), entryId);
    }

    @Override
    protected Page<PublicPage> findPages(UUID entryId, Long moment, int page, int size) {
        moment = moment != null ? moment : Long.MIN_VALUE;
        return publicPageRepository.findAllAfterMomentForEntry(requestContext.nodeId(), entryId, moment,
                PageRequest.of(page, size, Sort.Direction.ASC, "beforeMoment"));
    }

}
