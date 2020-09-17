package org.moera.node.operations;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

}
