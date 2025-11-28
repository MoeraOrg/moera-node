package org.moera.node.ui;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.UniversalLocation;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.operations.EmailVerificationOperations;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@UiController
@RequestMapping("/moera")
public class MoeraUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private EmailVerificationOperations emailVerificationOperations;

    @Inject
    private TitleBuilder titleBuilder;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @VirtualPage
    public String index() {
        return "redirect:/";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/complaints", produces = "text/html")
    @VirtualPage
    public String complaints(Model model) {
        return openClient("Complaints", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/complaints/{id}", produces = "text/html")
    @VirtualPage
    public String complaintsGroup(@PathVariable String id, Model model) {
        return openClient("Complaints", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/compose", produces = "text/html")
    @VirtualPage
    public String compose(Model model) {
        return openClient("New Post", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/connect", produces = "text/html")
    @VirtualPage
    public String connect(Model model) {
        return openClient("Sign In", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/explore", produces = "text/html")
    @VirtualPage
    public String explore(Model model) {
        return openClient("Recommendations", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/explore/people", produces = "text/html")
    @VirtualPage
    public String explorePeople(Model model) {
        return openClient("Top 50", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/grant", produces = "text/html")
    @VirtualPage
    public String grant(Model model) {
        return openClient("Grant", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/instants", produces = "text/html")
    @VirtualPage
    public String instants(Model model) {
        return openClient("Notifications", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/mnemonic", produces = "text/html")
    @VirtualPage
    public String mnemonic(Model model) {
        return openClient("Secret words", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/news", produces = "text/html")
    @VirtualPage
    public String news(Model model) {
        return openClient("News", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/people")
    @VirtualPage
    public String people() {
        return "redirect:/people/subscribers";
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/people/{tab}")
    @VirtualPage
    public String peopleTab(@PathVariable String tab, Model model) {
        if (!"subscribers".equals(tab) && !"subscriptions".equals(tab)) {
            return openClient("People", model);
        }
        return "redirect:/people/" + tab;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/post/{id}")
    @VirtualPage
    public String post(
        @PathVariable String id,
        @RequestParam(name = "comment", required = false) UUID commentId,
        @RequestParam(name = "media", required = false) UUID mediaId
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("redirect:/post/" + id);
        if (commentId != null) {
            builder = builder.queryParam("comment", commentId);
        }
        if (mediaId != null) {
            builder = builder.queryParam("media", mediaId);
        }
        return builder.build().toUriString();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/profile")
    @VirtualPage
    public String profile() {
        return "redirect:/profile";
    }

    @GetMapping("/profile/verify-email")
    @VirtualPage
    public String verifyEmail(@RequestParam(required = false) String token, Model model) {
        if (ObjectUtils.isEmpty(token)) {
            return openClient("Confirm your e-mail address", model);
        }

        boolean ok = requestContext.getOptions().getBool("profile.email.verified")
            || emailVerificationOperations.verified(token);
        return "redirect:"
            + UniversalLocation.redirectTo(requestContext.nodeName(), null, "/profile/email-verified", null, null);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/search", produces = "text/html")
    @VirtualPage
    public String search(Model model) {
        return openClient("Search", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/settings", produces = "text/html")
    @VirtualPage
    public String settings(Model model) {
        return openClient("Settings", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/settings/{tab}", produces = "text/html")
    @VirtualPage
    public String settingsTab(@PathVariable String tab, Model model) {
        return openClient("Settings", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/signup", produces = "text/html")
    @VirtualPage
    public String signUp(Model model) {
        return openClient("Sign Up", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/start-reading", produces = "text/html")
    @VirtualPage
    public String startReading(Model model) {
        return openClient("Start Reading", model);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/timeline")
    @VirtualPage
    public String timeline(@RequestParam(required = false) Long before) {
        return before == null ? "redirect:/timeline" : "redirect:/timeline?before=%d".formatted(before);
    }

    private String openClient(String title, Model model) {
        model.addAttribute("pageTitle", titleBuilder.build(title));

        return "client";
    }

}
