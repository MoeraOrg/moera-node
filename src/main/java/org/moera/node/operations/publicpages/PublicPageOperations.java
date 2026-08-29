package org.moera.node.operations.publicpages;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.util.LogUtil;
import org.moera.node.data.Entry;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.ui.PaginationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class PublicPageOperations {

    private static final Logger log = LoggerFactory.getLogger(PublicPageOperations.class);

    @Inject
    protected UniversalContext universalContext;

    @Inject
    protected PublicPageRepository publicPageRepository;

    protected int publicPageMaxSize;
    protected int publicPageAvgSize;

    protected PublicPageOperations(int publicPageMaxSize, int publicPageAvgSize) {
        this.publicPageMaxSize = publicPageMaxSize;
        this.publicPageAvgSize = publicPageAvgSize;
    }

    protected void updatePublicPages(UUID entryId, long moment) {
        int totalPages = countTotal(entryId);
        if (totalPages == 0) {
            var page = new PublicPage();
            page.setNodeId(universalContext.nodeId());
            page.setEntry(findEntryById(entryId));
            page.setAfterMoment(Long.MIN_VALUE);
            page.setBeforeMoment(Long.MAX_VALUE);
            publicPageRepository.save(page);
            return;
        }

        var containingPage = findContaining(entryId, moment);
        if (containingPage == null) {
            log.error("Public page does not exist for entry {} and moment {}", LogUtil.format(entryId), moment);
            return;
        }

        var after = containingPage.getAfterMoment();
        var before = containingPage.getBeforeMoment();
        int count = countInRange(entryId, after, before);
        if (count >= publicPageMaxSize) {
            long median = findMomentsInRange(
                entryId,
                after,
                before,
                PageRequest.of(count - publicPageAvgSize, 1, Sort.by(Sort.Direction.DESC, "moment"))
            ).getContent().getFirst();

            containingPage.setAfterMoment(median);

            var newPage = new PublicPage();
            newPage.setNodeId(universalContext.nodeId());
            newPage.setEntry(findEntryById(entryId));
            newPage.setAfterMoment(after);
            newPage.setBeforeMoment(median);
            publicPageRepository.save(newPage);
        }
    }

    protected abstract Entry findEntryById(UUID entryId);

    protected abstract PublicPage findContaining(UUID entryId, long moment);

    protected abstract int countInRange(UUID entryId, long after, long before);

    protected abstract Page<Long> findMomentsInRange(UUID entryId, long after, long before, Pageable pageable);

    public List<PaginationItem> createPagination(PublicPage page) {
        if (page == null) {
            return null;
        }
        UUID entryId = page.getEntry() != null ? page.getEntry().getId() : null;

        int current = countNumber(entryId, page.getBeforeMoment());
        int last = countTotal(entryId);
        if (last <= 1) {
            return null;
        }
        int tillLast = last - current;
        tillLast = Math.min(tillLast, 2);
        int rangeFirst = current + tillLast - 4;
        rangeFirst = Math.max(rangeFirst, 1);
        PublicPage firstPage = findPages(entryId, null, rangeFirst - 1, 1).getContent().getFirst();
        PublicPage lastPage = findPages(entryId, null, last - 1, 1).getContent().getFirst();
        List<PublicPage> pages = findPages(entryId, firstPage.getBeforeMoment(), 0, 5).getContent();
        int rangeLast = rangeFirst + pages.size() - 1;

        LinkedList<PaginationItem> items = new LinkedList<>();
        for (int i = 0; i < pages.size(); i++) {
            items.add(PaginationItem.pageLink(
                rangeFirst + i, pages.get(i).getBeforeMoment(), rangeFirst + i == current
            ));
        }
        if (rangeFirst > 2) {
            items.addFirst(PaginationItem.pageDots());
        }
        if (rangeFirst > 1) {
            items.addFirst(PaginationItem.pageLink(1, firstPage.getBeforeMoment(), false));
        }
        if (last - rangeLast > 1) {
            items.addLast(PaginationItem.pageDots());
        }
        if (last > rangeLast) {
            items.addLast(PaginationItem.pageLink(last, lastPage.getBeforeMoment(), false));
        }

        long prevMoment = 0;
        long nextMoment = 0;
        boolean afterCurrent = false;
        for (PaginationItem item : items) {
            if (item.dots()) {
                continue;
            }
            if (item.active()) {
                afterCurrent = true;
                continue;
            }
            if (!afterCurrent) {
                prevMoment = item.moment();
            } else {
                nextMoment = item.moment();
                break;
            }
        }
        if (prevMoment != 0) {
            items.addFirst(PaginationItem.pageLink("← Previous", prevMoment, false));
        }
        if (nextMoment != 0) {
            items.addLast(PaginationItem.pageLink("Next →", nextMoment, false));
        }
        return items;
    }

    protected abstract int countNumber(UUID entryId, long moment);

    protected abstract int countTotal(UUID entryId);

    protected abstract Page<PublicPage> findPages(UUID entryId, Long moment, int page, int size);

}
