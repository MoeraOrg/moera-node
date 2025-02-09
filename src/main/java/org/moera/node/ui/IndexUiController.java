package org.moera.node.ui;

import jakarta.inject.Inject;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.ProfileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@UiController
public class IndexUiController {

    private static final Logger log = LoggerFactory.getLogger(IndexUiController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/")
    @VirtualPage
    public String index() {
        return !requestContext.isRegistrar() ? "redirect:/timeline" : "redirect:/registrar";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/profile", produces = "text/html")
    @VirtualPage
    public String profile(Model model) {
        log.info("UI /profile");

        model.addAttribute("pageTitle", titleBuilder.build("Profile"));
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("profile", new ProfileInfo(requestContext.getPublic(), false));

        model.addAttribute("ogUrl", requestContext.getSiteUrl() + "/profile");
        model.addAttribute("ogTitle", "Profile");

        return "profile";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots() {
        if (requestContext.getOptions().getBool("webui.allow-indexing")) {
            return String.format(
                    "User-agent: *\n"
                    + "Disallow: /registrar\n"
                    + "Disallow: /moera/gotoname\n"
                    + "Disallow: /moera/news\n"
                    + "\n"
                    + "Sitemap: %s/sitemaps\n",
                    requestContext.getSiteUrl());
        } else {
            return "User-agent: *\n"
                    + "Disallow: /\n";
        }
    }

}
