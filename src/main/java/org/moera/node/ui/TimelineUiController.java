package org.moera.node.ui;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    private String timeline(Model model, @RequestParam(required = false) Long before) {
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

        return "timeline";
    }

}
