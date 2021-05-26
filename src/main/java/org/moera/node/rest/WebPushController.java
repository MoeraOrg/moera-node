package org.moera.node.rest;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.WebPushSubscription;
import org.moera.node.data.WebPushSubscriptionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.WebPushKey;
import org.moera.node.model.WebPushSubscriptionAttributes;
import org.moera.node.model.WebPushSubscriptionInfo;
import org.moera.node.util.Util;
import org.moera.node.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/web-push")
@NoCache
public class WebPushController {

    private static Logger log = LoggerFactory.getLogger(WebPushController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private WebPushService webPushService;

    @Inject
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

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

    @PostMapping("/subscriptions")
    @Admin
    @Transactional
    public WebPushSubscriptionInfo subscribe(
            @Valid @RequestBody WebPushSubscriptionAttributes webPushSubscriptionAttributes) {
        log.info("POST /web-push/subscriptions");

        WebPushSubscription subscription = webPushSubscriptionRepository.findByKeys(requestContext.nodeId(),
                webPushSubscriptionAttributes.getPublicKey(), webPushSubscriptionAttributes.getAuthKey()).orElse(null);
        if (subscription == null) {
            subscription = new WebPushSubscription();
            subscription.setId(UUID.randomUUID());
            subscription.setNodeId(requestContext.nodeId());
            webPushSubscriptionAttributes.toWebPushSubscription(subscription);
            webPushSubscriptionRepository.save(subscription);
        }

        return new WebPushSubscriptionInfo(subscription);
    }

    @DeleteMapping("/subscriptions/{id}")
    @Admin
    @Transactional
    public Result unsubscribe(@PathVariable UUID id) {
        log.info("DELETE /web-push/subscriptions/{id} (id = {})", LogUtil.format(id));

        WebPushSubscription subscription = webPushSubscriptionRepository.findById(id).orElse(null);
        if (subscription == null || !subscription.getNodeId().equals(requestContext.nodeId())) {
            throw new ObjectNotFoundFailure("web-push-subscription.not-found");
        }

        webPushSubscriptionRepository.delete(subscription);

        return Result.OK;
    }

}
