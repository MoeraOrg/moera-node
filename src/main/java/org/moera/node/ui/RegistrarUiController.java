package org.moera.node.ui;

import java.util.UUID;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.moera.node.domain.Domains;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.registrar.RegistrarHost;
import org.moera.node.util.UriUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@UiController
public class RegistrarUiController {

    private static final Pattern HOSTNAME = Pattern.compile("^[a-z][a-z0-9-]*$");

    @Value("${registrar.domain}")
    private String registrarDomain;

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    @GetMapping("/registrar")
    public String index(@RequestParam(required = false) String host, @RequestParam(required = false) String error,
                        Model model) {
        model.addAttribute("host", host);
        model.addAttribute("error", error);
        model.addAttribute("registrarDomain", registrarDomain);

        return "registrar/index";
    }

    @PostMapping(value = "/registrar/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String register(RegistrarHost registrarHost, RedirectAttributes attributes) {
        String error = createDomain(registrarHost.getHost() != null ? registrarHost.getHost().toLowerCase() : null);
        if (error != null) {
            attributes.addAttribute("host", registrarHost.getHost());
            attributes.addAttribute("error", error);
            return "redirect:/registrar";
        }
        attributes.addAttribute("host", getFullName(registrarHost.getHost()));
        return "redirect:/registrar/success";
    }

    private String createDomain(String hostName) {
        if (StringUtils.isEmpty(hostName)) {
            return "blank";
        }
        if (!HOSTNAME.matcher(hostName).matches()) {
            return "invalid";
        }

        String fullName = getFullName(hostName);
        domains.lockWrite();
        try {
            if (domains.getDomainNodeId(fullName) != null) {
                return "taken";
            }
            domains.createDomain(fullName, UUID.randomUUID());
        } finally {
            domains.unlockWrite();
        }

        return null;
    }

    private String getFullName(String hostName) {
        return hostName + "." + registrarDomain;
    }

    @GetMapping("/registrar/success")
    public String success(@RequestParam String host, HttpServletRequest request, Model model) {
        model.addAttribute("browserExtension", requestContext.isBrowserExtension());
        int port = UriUtil.createBuilderFromRequest(request).build().getPort();
        model.addAttribute("siteUrl", UriUtil.siteUrl(host, port));

        return "registrar/success";
    }

}
