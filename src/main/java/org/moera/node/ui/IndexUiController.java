package org.moera.node.ui;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.PublicRequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.RegisteredNameInfo;
import org.moera.node.option.Options;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class IndexUiController {

    @Inject
    private Options options;

    @GetMapping("/")
    @VirtualPage("/")
    private String index(Model model) {
        model.addAttribute("pageTitle", buildPageTitle("Timeline"));
        model.addAttribute("menuIndex", "index");

        return "index";
    }

    @GetMapping("/profile")
    @VirtualPage("/profile")
    public String profile(Model model, HttpServletResponse response) {
        model.addAttribute("pageTitle", buildPageTitle("Profile"));
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("registeredName", new RegisteredNameInfo(options, new PublicRequestContext()));
        model.addAttribute("profile", new ProfileInfo(options, new PublicRequestContext()));

        return "profile";
    }

    private CharSequence buildPageTitle(String title) {
        StringBuilder buf = new StringBuilder();
        if (!StringUtils.isEmpty(title)) {
            buf.append(title);
            buf.append(' ');
            String name = options.getString("profile.registered-name");
            if (!StringUtils.isEmpty(name)) {
                buf.append("@ ");
                buf.append(name);
                buf.append(' ');
            }
        }
        if (buf.length() > 0) {
            buf.append("| ");
        }
        buf.append("Moera");
        return buf;
    }

}
