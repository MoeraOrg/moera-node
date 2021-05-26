package org.moera.node.ui;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.NodeNameInfo;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = UiController.class)
public class GlobalsControllerAdvice {

    @Inject
    private RequestContext requestContext;

    @ModelAttribute
    public void session(Model model) {
        if (!requestContext.isRegistrar()) {
            model.addAttribute("nodeName", new NodeNameInfo(requestContext.getPublic()));
            model.addAttribute("nodeAvatar", requestContext.getPublic().getAvatar() != null
                    ? new AvatarImage(requestContext.getPublic().getAvatar()) : null);
            model.addAttribute("siteUrl", requestContext.getSiteUrl());
        }
    }

}
