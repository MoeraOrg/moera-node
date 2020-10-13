package org.moera.node.operations;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.ui.PaginationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class PublicPageOperations {

    @Inject
    protected RequestContext requestContext;

    @Inject
    protected PublicPageRepository publicPageRepository;

    protected int publicPageMaxSize;
    protected int publicPageAvgSize;

    protected PublicPageOperations(int publicPageMaxSize, int publicPageAvgSize) {
        this.publicPageMaxSize = publicPageMaxSize;
        this.publicPageAvgSize = publicPageAvgSize;
    }

    protected void updatePublicPages(UUID entryId, long moment) {
        PublicPage firstPage = findByBeforeMoment(entryId, Long.MAX_VALUE);
        if (firstPage == null) {
            firstPage = new PublicPage();
            firstPage.setNodeId(requestContext.nodeId());
            firstPage.setEntry(findEntryById(entryId));
            firstPage.setAfterMoment(Long.MIN_VALUE);
            firstPage.setBeforeMoment(Long.MAX_VALUE);
            publicPageRepository.save(firstPage);
            return;
        }

        long after = firstPage.getAfterMoment();
        if (moment > after) {
            int count = countInRange(entryId, after, Long.MAX_VALUE);
            if (count >= publicPageMaxSize) {
                long median = findMomentsInRange(entryId, after, Long.MAX_VALUE,
                        PageRequest.of(count - publicPageAvgSize, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                firstPage.setAfterMoment(median);
                PublicPage secondPage = new PublicPage();
                secondPage.setNodeId(requestContext.nodeId());
                secondPage.setEntry(findEntryById(entryId));
                secondPage.setAfterMoment(after);
                secondPage.setBeforeMoment(median);
                publicPageRepository.save(secondPage);
            }
            return;
        }

        PublicPage lastPage = findByAfterMoment(entryId, Long.MIN_VALUE);
        long end = lastPage.getBeforeMoment();
        if (moment <= end) {
            int count = countInRange(entryId, Long.MIN_VALUE, end);
            if (count >= publicPageMaxSize) {
                long median = findMomentsInRange(entryId, Long.MIN_VALUE, end,
                        PageRequest.of(publicPageAvgSize + 1, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                lastPage.setBeforeMoment(median);
                PublicPage prevPage = new PublicPage();
                prevPage.setNodeId(requestContext.nodeId());
                prevPage.setEntry(findEntryById(entryId));
                prevPage.setAfterMoment(median);
                prevPage.setBeforeMoment(end);
                publicPageRepository.save(prevPage);
            }
        }
    }

    protected abstract Entry findEntryById(UUID entryId);

    protected abstract PublicPage findByBeforeMoment(UUID entryId, long before);

    protected abstract PublicPage findByAfterMoment(UUID entryId, long after);

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
        PublicPage firstPage = findPages(entryId, null, rangeFirst - 1, 1).getContent().get(0);
        PublicPage lastPage = findPages(entryId, null, last - 1, 1).getContent().get(0);
        List<PublicPage> pages = findPages(entryId, firstPage.getBeforeMoment(), 0, 5).getContent();
        int rangeLast = rangeFirst + pages.size() - 1;

        LinkedList<PaginationItem> items = new LinkedList<>();
        for (int i = 0; i < pages.size(); i++) {
            items.add(PaginationItem.pageLink(rangeFirst + i, pages.get(i).getBeforeMoment(),
                    rangeFirst + i == current));
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
            if (item.isDots()) {
                continue;
            }
            if (item.isActive()) {
                afterCurrent = true;
                continue;
            }
            if (!afterCurrent) {
                prevMoment = item.getMoment();
            } else {
                nextMoment = item.getMoment();
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
