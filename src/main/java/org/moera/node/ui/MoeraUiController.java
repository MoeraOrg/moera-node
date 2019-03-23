package org.moera.node.ui;

import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

    @GetMapping
    @VirtualPage
    public String index() {
        return "redirect:/";
    }

    @GetMapping("/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

}
