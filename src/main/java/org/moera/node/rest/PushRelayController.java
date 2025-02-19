package org.moera.node.rest;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.PushRelayClientAttributes;
import org.moera.lib.node.types.PushRelayType;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.pushrelay.FcmRelay;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/push-relay")
@NoCache
public class PushRelayController {

    private static final Logger log = LoggerFactory.getLogger(PushRelayController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private FcmRelay fcmRelay;

    @PostMapping
    @Admin(Scope.OTHER)
    @Entitled
    public Result post(@RequestBody PushRelayClientAttributes pushRelayClientAttributes) {
        log.info(
            "POST /push-relay (type = {}, clientId = {}, lang = {})",
            LogUtil.format(PushRelayType.toValue(pushRelayClientAttributes.getType())),
            LogUtil.format(pushRelayClientAttributes.getClientId(), 6),
            LogUtil.format(pushRelayClientAttributes.getLang())
        );

        pushRelayClientAttributes.validate();
        ValidationUtil.assertion(
            pushRelayClientAttributes.getType() == PushRelayType.FCM,
            "push-relay.type.unknown"
        );

        long now = Instant.now().getEpochSecond();
        byte[] fingerprint = Fingerprints.pushRelayRegister(
            pushRelayClientAttributes.getClientId(),
            pushRelayClientAttributes.getLang(),
            Util.toTimestamp(now)
        );
        byte[] signature = CryptoUtil.sign(fingerprint, getSigningKey());
        try {
            fcmRelay.register(
                pushRelayClientAttributes.getClientId(),
                requestContext.nodeName(),
                pushRelayClientAttributes.getLang(),
                now,
                signature
            );
        } catch (Exception e) {
            log.warn("Error calling push relay service: " + e.getMessage());
            throw new OperationFailure("push-relay.error");
        }

        requestContext.getOptions().set("push-relay.fcm.active", true);

        return Result.OK;
    }

    private ECPrivateKey getSigningKey() {
        return (ECPrivateKey) requestContext.getOptions().getPrivateKey("profile.signing-key");
    }

}
