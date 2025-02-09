package org.moera.node.ui;

import jakarta.inject.Inject;

import org.moera.lib.naming.NodeName;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.NodeNameInfo;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = UiController.class)
public class GlobalsControllerAdvice {

    @Inject
    private RequestContext requestContext;

    @ModelAttribute
    public void session(Model model) {
        model.addAttribute("webClientUrl", requestContext.getRedirectorUrl());
        if (!requestContext.isRegistrar()) {
            model.addAttribute("nodeName", new NodeNameInfo(requestContext.getPublic()));
            model.addAttribute("nodeAvatar", requestContext.getPublic().getAvatar() != null
                    ? new AvatarImage(requestContext.getPublic().getAvatar()) : null);
            model.addAttribute("siteUrl", requestContext.getSiteUrl());
        }
        model.addAttribute("ogType", "website");
        if (requestContext.getAvatar() != null) {
            AvatarImage avatarImage = new AvatarImage(requestContext.getAvatar());
            model.addAttribute("ogImage", requestContext.getSiteUrl() + "/moera/media/" + avatarImage.getPath());
            model.addAttribute("ogImageType", avatarImage.getMediaFile().getMimeType());
            model.addAttribute("ogImageWidth", avatarImage.getWidth());
            model.addAttribute("ogImageHeight", avatarImage.getHeight());
        } else {
            model.addAttribute("ogImage", requestContext.getSiteUrl() + "/pics/avatar.png");
            model.addAttribute("ogImageType", "image/png");
            model.addAttribute("ogImageWidth", 200);
            model.addAttribute("ogImageHeight", 200);
        }
        if (requestContext.getOptions() != null) {
            model.addAttribute("ogDescription", requestContext.getOptions().getString("profile.title"));
        }
        String siteName = !ObjectUtils.isEmpty(requestContext.fullName())
                ? requestContext.fullName() : NodeName.shorten(requestContext.nodeName());
        model.addAttribute("ogSiteName", siteName);
        model.addAttribute("ogTitle", siteName);
    }

}
