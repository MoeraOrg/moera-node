package org.moera.node.ui;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@UiController
public class TimelineUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

    @Inject
    private PublicPageRepository publicPageRepository;

    @Inject
    private PostingRepository postingRepository;

    @GetMapping("/timeline")
    @VirtualPage("/timeline")
    public String timeline(@RequestParam(required = false) Long before, Model model) {
        before = before != null ? before : Long.MAX_VALUE;
        List<Posting> postings = Collections.emptyList();
        PublicPage publicPage = publicPageRepository.findContaining(requestContext.nodeId(), before);
        if (publicPage != null) {
            if (publicPage.getEndMoment() != before) {
                if (publicPage.getEndMoment() != Long.MAX_VALUE) {
                    return String.format("redirect:/timeline?before=%d#m%d", publicPage.getEndMoment(), before);
                } else {
                    return String.format("redirect:/timeline#m%d", before);
                }
            }
            postings = postingRepository.findInRange(
                    requestContext.nodeId(), publicPage.getBeginMoment(), publicPage.getEndMoment());
        }

        model.addAttribute("pageTitle", titleBuilder.build("Timeline"));
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("postings", postings);
        model.addAttribute("pagination", createPagination(publicPage));

        return "timeline";
    }

    private List<PaginationItem> createPagination(PublicPage page) {
        if (page == null) {
            return null;
        }

        int current = publicPageRepository.countNumber(requestContext.nodeId(), page.getEndMoment());
        int last = publicPageRepository.countTotal(requestContext.nodeId());
        if (last <= 1) {
            return null;
        }
        int tillLast = last - current;
        tillLast = tillLast > 2 ? 2 : tillLast;
        int rangeFirst = current + tillLast - 4;
        rangeFirst = rangeFirst < 1 ? 1 : rangeFirst;
        PublicPage firstPage = publicPageRepository.findAllBeforeEndMoment(
                requestContext.nodeId(), Long.MAX_VALUE,
                PageRequest.of(rangeFirst - 1, 1, Sort.Direction.DESC, "endMoment"))
                .getContent().get(0);
        PublicPage lastPage = publicPageRepository.findAllBeforeEndMoment(
                requestContext.nodeId(), Long.MAX_VALUE,
                PageRequest.of(last - 1, 1, Sort.Direction.DESC, "endMoment"))
                .getContent().get(0);
        List<PublicPage> pages = publicPageRepository.findAllBeforeEndMoment(
                requestContext.nodeId(), firstPage.getEndMoment(),
                PageRequest.of(rangeFirst - 1, 5, Sort.Direction.DESC, "endMoment"))
                .getContent();
        int rangeLast = rangeFirst + pages.size() - 1;

        LinkedList<PaginationItem> items = new LinkedList<>();
        for (int i = 0; i < pages.size(); i++) {
            items.add(PaginationItem.pageLink(rangeFirst + i, pages.get(i).getEndMoment(), rangeFirst + i == current));
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
            items.addLast(PaginationItem.pageLink(last, lastPage.getEndMoment(), false));
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

    @GetMapping("/post/{id}")
    public String posting(@PathVariable UUID id, Model model) {
        Posting posting = postingRepository.findById(id).orElse(null);
        if (posting == null) {
            throw new PageNotFoundException();
        }

        model.addAttribute("pageTitle", titleBuilder.build("Posting")); // FIXME extract title from text
        model.addAttribute("menuIndex", "timeline");
        model.addAttribute("posting", posting);

        return "posting";
    }

}
