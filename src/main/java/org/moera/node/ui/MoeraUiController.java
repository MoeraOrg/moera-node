package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredNameDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

    @Inject
    private NamingCache namingCache;

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
    public String compose() {
        throw new PageNotFoundException();
    }

    @GetMapping("/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

    @GetMapping("/settings")
    @VirtualPage
    public String settings() {
        return "redirect:/settings";
    }

    @GetMapping("/settings/{tab}")
    @VirtualPage
    public String settingsTab(@PathVariable String tab) {
        return "redirect:/settings/" + tab;
    }

    @GetMapping("/gotoname")
    public String goToName(@RequestParam String name, @RequestParam(required = false) String location) {
        RegisteredNameDetails details = namingCache.get(name);
        if (details == null || details.getNodeUri() == null) {
            throw new PageNotFoundException();
        }
        return "redirect:" + details.getNodeUri() + (location != null ? location : "");
    }

}
