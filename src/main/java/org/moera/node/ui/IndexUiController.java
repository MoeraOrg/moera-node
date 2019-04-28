package org.moera.node.ui;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.RegisteredNameInfo;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class IndexUiController {

    @Inject
    private RequestContext requestContext;

    @GetMapping("/")
    @VirtualPage("/")
    private String index(Model model) {
        return "redirect:/timeline";
    }

    @GetMapping("/timeline")
    @VirtualPage("/timeline")
    private String timeline(Model model) {
        model.addAttribute("pageTitle", buildPageTitle("Timeline"));
        model.addAttribute("menuIndex", "timeline");

        return "timeline";
    }

    @GetMapping("/profile")
    @VirtualPage("/profile")
    public String profile(Model model, HttpServletResponse response) {
        model.addAttribute("pageTitle", buildPageTitle("Profile"));
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("registeredName", new RegisteredNameInfo(requestContext.getPublic()));
        model.addAttribute("profile", new ProfileInfo(requestContext.getPublic()));

        return "profile";
    }

    private CharSequence buildPageTitle(String title) {
        StringBuilder buf = new StringBuilder();
        if (!StringUtils.isEmpty(title)) {
            buf.append(title);
            buf.append(' ');
            String name = requestContext.getPublic().getOptions().getString("profile.registered-name");
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
