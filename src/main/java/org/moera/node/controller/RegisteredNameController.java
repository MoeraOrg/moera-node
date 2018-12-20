package org.moera.node.controller;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.inject.Inject;

import org.moera.node.model.RegisteredName;
import org.moera.node.naming.NamingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @Inject
    private NamingClient namingClient;

    @PostMapping("/moera-node/registered-name")
    public void post(@RequestBody RegisteredName registeredName) throws NoSuchAlgorithmException { // TODO handle it
        log.info("Asked to register the name '{}'", registeredName.getName());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstanceStrong();
        keyPairGenerator.initialize(256, random);
        KeyPair pair = keyPairGenerator.generateKeyPair();
        namingClient.register(registeredName.getName(), pair.getPublic());
    }

}
