package org.moera.node.ui;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

    @Inject
    private TitleBuilder titleBuilder;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @VirtualPage
    public String index() {
        return "redirect:/";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/timeline")
    @VirtualPage
    public String timeline(@RequestParam(required = false) Long before) {
        return before == null ? "redirect:/timeline" : "redirect:/timeline?before=%d".formatted(before);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/post/{id}")
    @VirtualPage
    public String post(@PathVariable String id,
                       @RequestParam(name = "comment", required = false) UUID commentId,
                       @RequestParam(name = "media", required = false) UUID mediaId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("redirect:/post/" + id);
        if (commentId != null) {
            builder = builder.queryParam("comment", commentId);
        }
        if (mediaId != null) {
            builder = builder.queryParam("media", mediaId);
        }
        return builder.build().toUriString();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/compose", produces = "text/html")
    @VirtualPage
    public String compose(Model model) {
        return openClient("New Post", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/settings", produces = "text/html")
    @VirtualPage
    public String settings(Model model) {
        return openClient("Settings", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/settings/{tab}", produces = "text/html")
    @VirtualPage
    public String settingsTab(@PathVariable String tab, Model model) {
        return openClient("Settings", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/news", produces = "text/html")
    @VirtualPage
    public String news(Model model) {
        return openClient("News", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/people")
    @VirtualPage
    public String people() {
        return "redirect:/people/subscribers";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/people/{tab}")
    @VirtualPage
    public String peopleTab(@PathVariable String tab) {
        return "redirect:/people/" + tab;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/complaints", produces = "text/html")
    @VirtualPage
    public String complaints(Model model) {
        return openClient("Complaints", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/complaints/{id}", produces = "text/html")
    @VirtualPage
    public String complaintsGroup(@PathVariable String id, Model model) {
        return openClient("Complaints", model);
    }

    private String openClient(String title, Model model) {
        model.addAttribute("pageTitle", titleBuilder.build(title));

        return "client";
    }

}
