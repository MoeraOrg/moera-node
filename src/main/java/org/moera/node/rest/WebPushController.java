package org.moera.node.rest;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.WebPushKey;
import org.moera.node.util.Util;
import org.moera.node.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/web-push")
public class WebPushController {

    private static Logger log = LoggerFactory.getLogger(WebPushController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private WebPushService webPushService;

    @GetMapping("/key")
    @Admin
    public WebPushKey getKey() {
        log.info("GET /web-push/key");

        PublicKey key = requestContext.getOptions().getPublicKey("web-push.public-key");
        if (key == null) {
            key = webPushService.generateKeys(requestContext.getOptions());
        }
        return new WebPushKey(Util.base64encode(CryptoUtil.toUncompressedPublicKey((ECPublicKey) key)));
    }

}
