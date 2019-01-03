package org.moera.node.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexUiController {

    @GetMapping("/")
    private String index(Model model) {
        model.addAttribute("menuIndex", "index");

        return "index";
    }

}
