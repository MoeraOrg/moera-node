package org.moera.node.rest;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;

import io.github.novacrypto.bip39.JavaxPBKDF2WithHmacSHA512;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.moera.commons.crypto.CryptoException;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.Rules;
import org.moera.node.event.EventManager;
import org.moera.node.event.model.NodeNameChangedEvent;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.NameToRegister;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.NodeNameInfo;
import org.moera.node.model.RegisteredNameSecret;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.option.Options;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/node-name")
public class NodeNameController {

    private static Logger log = LoggerFactory.getLogger(NodeNameController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingClient namingClient;

    @Inject
    private EventManager eventManager;

    @GetMapping
    public NodeNameInfo get() {
        log.info("GET /node-name");

        return new NodeNameInfo(requestContext);
    }

    @PostMapping
    @Admin
    @Transactional
    public RegisteredNameSecret post(@Valid @RequestBody NameToRegister nameToRegister, HttpServletRequest request) {
        if (!Rules.isNameValid(nameToRegister.getName())) {
            throw new OperationFailure("nameToRegister.name.invalid");
        }

        log.info("POST /node-name (name = '{}')", nameToRegister.getName());

        Options options = requestContext.getOptions();
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        RegisteredNameSecret secretInfo = new RegisteredNameSecret();
        secretInfo.setName(nameToRegister.getName());
        KeyPair signingKeyPair;
        try {
            SecureRandom random = new SecureRandom();

            byte[] entropy = new byte[Words.TWENTY_FOUR.byteLength()];
            random.nextBytes(entropy);
            StringBuilder mnemonic = new StringBuilder();
            new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, mnemonic::append);
            byte[] seed = new SeedCalculator(JavaxPBKDF2WithHmacSHA512.INSTANCE).calculateSeed(mnemonic.toString(), "");

            secretInfo.setSecret(Util.base64encode(entropy));
            secretInfo.setMnemonic(mnemonic.toString().split(" "));

            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Rules.EC_CURVE);

            BigInteger d = new BigInteger(seed);
            ECPoint q = ecSpec.getG().multiply(d);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(q, ecSpec);
            ECPublicKey publicUpdatingKey = (ECPublicKey) keyFactory.generatePublic(pubSpec);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, random);
            signingKeyPair = keyPairGenerator.generateKeyPair();

            namingClient.register(
                    nameToRegister.getName(),
                    UriUtil.createBuilderFromRequest(request).replacePath("/moera").replaceQuery(null).toUriString(),
                    publicUpdatingKey,
                    (ECPrivateKey) signingKeyPair.getPrivate(),
                    (ECPublicKey) signingKeyPair.getPublic(),
                    options);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }

        return secretInfo;
    }

    @PutMapping
    @Admin
    @Transactional
    public Result put(@Valid @RequestBody RegisteredNameSecret registeredNameSecret) {
        log.info("PUT /node-name");

        Options options = requestContext.getOptions();
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        String nodeName = registeredNameSecret.getName() != null ? registeredNameSecret.getName() : options.nodeName();
        RegisteredName registeredName = (RegisteredName) NodeName.parse(nodeName);

        if (StringUtils.isEmpty(registeredName.getName()) || registeredName.getGeneration() == null) {
            throw new ValidationFailure("node-name.name-absent");
        }
        if ((registeredNameSecret.getMnemonic() == null || registeredNameSecret.getMnemonic().length == 0)
                && StringUtils.isEmpty(registeredNameSecret.getSecret())) {
            throw new ValidationFailure("registeredNameSecret.empty");
        }

        String mnemonic;
        if (!StringUtils.isEmpty(registeredNameSecret.getSecret())) {
            byte[] entropy = Util.base64decode(registeredNameSecret.getSecret());
            StringBuilder buf = new StringBuilder();
            new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, buf::append);
            mnemonic = buf.toString();
        } else {
            mnemonic = String.join(" ", registeredNameSecret.getMnemonic());
        }
        byte[] seed = new SeedCalculator(JavaxPBKDF2WithHmacSHA512.INSTANCE).calculateSeed(mnemonic, "");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

            BigInteger d = new BigInteger(seed);
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(d, ecSpec);
            ECPrivateKey privateUpdatingKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            ECPrivateKey privateSigningKey = null;
            ECPublicKey signingKey = null;
            if (registeredNameSecret.getName() != null) {
                SecureRandom random = new SecureRandom();
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
                keyPairGenerator.initialize(ecSpec, random);
                KeyPair signingKeyPair = keyPairGenerator.generateKeyPair();
                privateSigningKey = (ECPrivateKey) signingKeyPair.getPrivate();
                signingKey = (ECPublicKey) signingKeyPair.getPublic();
            }

            namingClient.update(registeredName.getName(), registeredName.getGeneration(), privateUpdatingKey,
                    privateSigningKey, signingKey, options);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        } catch (OperationFailure of) {
            throw new OperationFailure("node-name." + of.getErrorCode());
        }

        return Result.OK;
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result delete() {
        log.info("DELETE /node-name");

        if (requestContext.getOptions().getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        requestContext.getOptions().runInTransaction(options -> {
            options.reset("profile.node-name");
            options.reset("profile.signing-key");
        });
        eventManager.send(new NodeNameChangedEvent(""));

        return Result.OK;
    }

}
