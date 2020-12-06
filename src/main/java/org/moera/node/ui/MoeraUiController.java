package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.util.Util;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

    @GetMapping
    @VirtualPage
    public String index() {
        return "redirect:/";
    }

    @GetMapping("/timeline")
    @VirtualPage
    public String timeline(@RequestParam(required = false) Long before) {
        return before == null ? "redirect:/timeline" : String.format("redirect:/timeline?before=%d", before);
    }

    @GetMapping("/post/{id}")
    @VirtualPage
    public String post(@PathVariable String id) {
        return "redirect:/post/" + id;
    }

    @GetMapping("/compose")
    @VirtualPage
    public String compose(Model model) {
        return openClient("New Post", model);
    }

    @GetMapping("/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

    @GetMapping("/settings")
    @VirtualPage
    public String settings(Model model) {
        return openClient("Settings", model);
    }

    @GetMapping("/settings/{tab}")
    @VirtualPage
    public String settingsTab(@PathVariable String tab, Model model) {
        return openClient("Settings", model);
    }

    @GetMapping("/news")
    @VirtualPage
    public String news(Model model) {
        return openClient("News", model);
    }

    @GetMapping("/people")
    @VirtualPage
    public String people() {
        return "redirect:/people/subscribers";
    }

    @GetMapping("/people/{tab}")
    @VirtualPage
    public String peopleTab(@PathVariable String tab) {
        return "redirect:/people/" + tab;
    }

    private String openClient(String title, Model model) {
        model.addAttribute("pageTitle", titleBuilder.build(title));
        model.addAttribute("url", Util.ue(requestContext.getUrl()));

        return "client";
    }

}
