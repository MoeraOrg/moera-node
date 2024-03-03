package org.moera.node.ui;

import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.UniversalLocation;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.api.naming.NamingCache;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ** For backward compatibility only **

@Controller
@RequestMapping("/moera")
public class RedirectController {

    @Inject
    private NamingCache namingCache;

    @GetMapping("/gotoname")
    @Transactional
    public String goToName(@RequestParam(required = false) String name,
                           @RequestParam(required = false) String location) {
        String nodeUrl = null;
        if (!ObjectUtils.isEmpty(name)) {
            nodeUrl = namingCache.getFast(name).getNodeUri();
        } else {
            name = null;
        }
        String target;
        if (location != null) {
            try {
                URI uri = new URI(location);
                target = UniversalLocation.redirectTo(name, nodeUrl, uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw new PageNotFoundException();
            }
        } else {
            target = UniversalLocation.redirectTo(name, nodeUrl, null, null, null);
        }
        return "redirect:" + target;
    }

}
