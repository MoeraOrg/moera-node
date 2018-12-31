package org.moera.node.rest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.inject.Inject;

import org.moera.commons.util.CryptoException;
import org.moera.node.global.Admin;
import org.moera.node.model.NameToRegister;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.naming.NamingClient;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera-node/registered-name")
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @Inject
    private Options options;

    @Inject
    private NamingClient namingClient;

    @PostMapping
    @Admin
    @ResponseBody
    public Result post(@RequestBody NameToRegister nameToRegister) {
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("nameToRegister.operation-pending");
        }
        KeyPair signingKeyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            SecureRandom random = SecureRandom.getInstanceStrong();
            keyPairGenerator.initialize(256, random);
            KeyPair updatingKeyPair = keyPairGenerator.generateKeyPair();
            keyPairGenerator.initialize(256, random);
            signingKeyPair = keyPairGenerator.generateKeyPair();
            namingClient.register(nameToRegister.getName(), updatingKeyPair.getPublic(), signingKeyPair.getPublic());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        options.set("profile.signing-key", signingKeyPair.getPrivate());

        return Result.OK;
    }

}
