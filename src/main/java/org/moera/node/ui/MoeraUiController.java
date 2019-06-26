package org.moera.node.ui;

import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

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

    @GetMapping("/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

}
