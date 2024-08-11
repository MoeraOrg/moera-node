package org.moera.node.rest;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.api.pushrelay.FcmRelay;
import org.moera.node.api.pushrelay.PushRelayRegisterFingerprint;
import org.moera.node.auth.Admin;
import org.moera.node.auth.Scope;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PushRelayClientAttributes;
import org.moera.node.model.PushRelayType;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
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
    public Result post(@Valid @RequestBody PushRelayClientAttributes pushRelayClientAttributes) {
        log.info("POST /push-relay (type = {}, clientId = {}, lang = {})",
                LogUtil.format(PushRelayType.toValue(pushRelayClientAttributes.getType())),
                LogUtil.format(pushRelayClientAttributes.getClientId(), 6),
                LogUtil.format(pushRelayClientAttributes.getLang()));

        if (pushRelayClientAttributes.getType() == null) {
            throw new ValidationFailure("pushRelayClientAttributes.type.blank");
        }
        if (pushRelayClientAttributes.getType() != PushRelayType.FCM) {
            throw new ValidationFailure("pushRelayClientAttributes.type.unknown");
        }

        long now = Instant.now().getEpochSecond();
        PushRelayRegisterFingerprint fingerprint = new PushRelayRegisterFingerprint(
                pushRelayClientAttributes.getClientId(),
                pushRelayClientAttributes.getLang(),
                now);
        byte[] signature = CryptoUtil.sign(fingerprint, getSigningKey());
        try {
            fcmRelay.register(
                    pushRelayClientAttributes.getClientId(),
                    requestContext.nodeName(),
                    pushRelayClientAttributes.getLang(),
                    now,
                    signature);
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
