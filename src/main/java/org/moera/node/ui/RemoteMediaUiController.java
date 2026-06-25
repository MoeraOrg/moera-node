package org.moera.node.ui;

import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.node.api.naming.NamingCache;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/moera/remote-media/{nodeName}")
public class RemoteMediaUiController {

    @Inject
    private NamingCache namingCache;

    @GetMapping("/private/{id}.{ext}")
    @Transactional
    public String getDataPrivate(
        @PathVariable String nodeName,
        @PathVariable String id,
        @PathVariable String ext,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download,
        @RequestParam(name = "grant", required = false) String grantS,
        @RequestParam(name = "ignoremalware", required = false) Boolean ignoreMalware
    ) {
        String nodeUrl = namingCache.get(nodeName).getNodeUri();
        var uriBuilder = UriComponentsBuilder.fromUriString(nodeUrl + "/media/private/" + id + "." + ext);
        uriBuilder.queryParamIfPresent("width", Optional.ofNullable(width));
        uriBuilder.queryParamIfPresent("download", Optional.ofNullable(download));
        uriBuilder.queryParamIfPresent("grant", Optional.ofNullable(grantS));
        uriBuilder.queryParamIfPresent("ignoremalware", Optional.ofNullable(ignoreMalware));
        return "redirect:" + uriBuilder.toUriString();
    }

}
