package org.moera.node.operations;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.ui.PaginationItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PublicPageOperations {

    private static final int PUBLIC_PAGE_MAX_SIZE = 30;
    private static final int PUBLIC_PAGE_AVG_SIZE = 20;

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    public void updatePublicPages(long moment) {
        UUID nodeId = requestContext.nodeId();
        PublicPage firstPage = publicPageRepository.findByBeforeMoment(nodeId, Long.MAX_VALUE);
        if (firstPage == null) {
            firstPage = new PublicPage();
            firstPage.setNodeId(requestContext.nodeId());
            firstPage.setAfterMoment(Long.MIN_VALUE);
            firstPage.setBeforeMoment(Long.MAX_VALUE);
            publicPageRepository.save(firstPage);
            return;
        }

        long after = firstPage.getAfterMoment();
        if (moment > after) {
            int count = storyRepository.countInRange(nodeId, Feed.TIMELINE, after, Long.MAX_VALUE);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = storyRepository.findMomentsInRange(nodeId, Feed.TIMELINE, after, Long.MAX_VALUE,
                        PageRequest.of(count - PUBLIC_PAGE_AVG_SIZE, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                firstPage.setAfterMoment(median);
                PublicPage secondPage = new PublicPage();
                secondPage.setNodeId(requestContext.nodeId());
                secondPage.setAfterMoment(after);
                secondPage.setBeforeMoment(median);
                publicPageRepository.save(secondPage);
            }
            return;
        }

        PublicPage lastPage = publicPageRepository.findByAfterMoment(nodeId, Long.MIN_VALUE);
        long end = lastPage.getBeforeMoment();
        if (moment <= end) {
            int count = storyRepository.countInRange(nodeId, Feed.TIMELINE, Long.MIN_VALUE, end);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = storyRepository.findMomentsInRange(nodeId, Feed.TIMELINE, Long.MIN_VALUE, end,
                        PageRequest.of(PUBLIC_PAGE_AVG_SIZE + 1, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                lastPage.setBeforeMoment(median);
                PublicPage prevPage = new PublicPage();
                prevPage.setNodeId(requestContext.nodeId());
                prevPage.setAfterMoment(median);
                prevPage.setBeforeMoment(end);
                publicPageRepository.save(prevPage);
            }
        }
    }

    public List<PaginationItem> createPagination(PublicPage page) {
        if (page == null) {
            return null;
        }

        int current = publicPageRepository.countNumber(requestContext.nodeId(), page.getBeforeMoment());
        int last = publicPageRepository.countTotal(requestContext.nodeId());
        if (last <= 1) {
            return null;
        }
        int tillLast = last - current;
        tillLast = Math.min(tillLast, 2);
        int rangeFirst = current + tillLast - 4;
        rangeFirst = Math.max(rangeFirst, 1);
        PublicPage firstPage = publicPageRepository.findAllBeforeMoment(
                requestContext.nodeId(), Long.MAX_VALUE,
                PageRequest.of(rangeFirst - 1, 1, Sort.Direction.DESC, "beforeMoment"))
                .getContent().get(0);
        PublicPage lastPage = publicPageRepository.findAllBeforeMoment(
                requestContext.nodeId(), Long.MAX_VALUE,
                PageRequest.of(last - 1, 1, Sort.Direction.DESC, "beforeMoment"))
                .getContent().get(0);
        List<PublicPage> pages = publicPageRepository.findAllBeforeMoment(
                requestContext.nodeId(), firstPage.getBeforeMoment(),
                PageRequest.of(0, 5, Sort.Direction.DESC, "beforeMoment"))
                .getContent();
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
            items.addFirst(PaginationItem.pageLink(1, Long.MAX_VALUE, false));
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

}
