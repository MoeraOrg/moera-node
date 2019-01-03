package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.model.Profile;
import org.moera.node.option.Options;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileUiController {

    @Inject
    private Options options;

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("profile", new Profile(options));

        return "profile";
    }

}
