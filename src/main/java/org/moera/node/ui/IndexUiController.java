package org.moera.node.ui;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.ProfileInfo;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class IndexUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private TitleBuilder titleBuilder;

    @GetMapping("/")
    @VirtualPage
    private String index(Model model) {
        return !requestContext.isRegistrar() ? "redirect:/timeline" : "redirect:/registrar";
    }

    @GetMapping("/profile")
    @VirtualPage
    public String profile(Model model, HttpServletResponse response) {
        Avatar avatar = avatarRepository.findByNodeIdAndCurrent(requestContext.nodeId()).orElse(null);

        model.addAttribute("pageTitle", titleBuilder.build("Profile"));
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("profile", new ProfileInfo(requestContext.getPublic(), avatar, false));

        return "profile";
    }

}
