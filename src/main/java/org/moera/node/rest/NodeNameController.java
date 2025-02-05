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

import io.github.novacrypto.bip39.JavaxPbkdf2WithHmacSha512;
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
import org.moera.lib.Rules;
import org.moera.node.api.naming.NamingClient;
import org.moera.node.auth.Admin;
import org.moera.node.auth.Scope;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.model.KeyMnemonic;
import org.moera.node.model.NameToRegister;
import org.moera.node.model.NodeNameInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RegisteredNameSecret;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.option.Options;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/node-name")
@NoCache
public class NodeNameController {

    private static final Logger log = LoggerFactory.getLogger(NodeNameController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingClient namingClient;

    @GetMapping
    public NodeNameInfo get() {
        log.info("GET /node-name");

        return new NodeNameInfo(requestContext);
    }

    @PostMapping
    @Admin(Scope.NAME)
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
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Rules.EC_CURVE);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");

            BigInteger p = ecSpec.getCurve().getField().getCharacteristic();
            BigInteger d = BigInteger.ZERO;
            while (d.equals(BigInteger.ZERO)) {
                random.nextBytes(entropy);
                StringBuilder mnemonic = new StringBuilder();
                new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, mnemonic::append);
                byte[] seed = new SeedCalculator(JavaxPbkdf2WithHmacSha512.INSTANCE)
                        .calculateSeed(mnemonic.toString(), "");

                secretInfo.setSecret(Util.base64encode(entropy));
                secretInfo.setMnemonic(mnemonic.toString().split(" "));

                d = new BigInteger(1, seed).remainder(p);
            }
            ECPoint q = ecSpec.getG().multiply(d);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(q, ecSpec);
            ECPublicKey publicUpdatingKey = (ECPublicKey) keyFactory.generatePublic(pubSpec);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, random);
            signingKeyPair = keyPairGenerator.generateKeyPair();

            namingClient.register(
                    nameToRegister.getName(),
                    getNodeUri(request),
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
    @Admin(Scope.NAME)
    @Transactional
    public Result put(@Valid @RequestBody RegisteredNameSecret registeredNameSecret, HttpServletRequest request) {
        log.info("PUT /node-name");

        Options options = requestContext.getOptions();
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        String nodeName = registeredNameSecret.getName() != null ? registeredNameSecret.getName() : options.nodeName();
        if (ObjectUtils.isEmpty(nodeName)) {
            throw new ValidationFailure("node-name.name-absent");
        }
        if ((registeredNameSecret.getMnemonic() == null || registeredNameSecret.getMnemonic().length == 0)
                && ObjectUtils.isEmpty(registeredNameSecret.getSecret())) {
            throw new ValidationFailure("registeredNameSecret.empty");
        }

        String mnemonic;
        if (!ObjectUtils.isEmpty(registeredNameSecret.getSecret())) {
            byte[] entropy = Util.base64decode(registeredNameSecret.getSecret());
            StringBuilder buf = new StringBuilder();
            new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, buf::append);
            mnemonic = buf.toString();
        } else {
            mnemonic = String.join(" ", registeredNameSecret.getMnemonic());
        }
        byte[] seed = new SeedCalculator(JavaxPbkdf2WithHmacSha512.INSTANCE).calculateSeed(mnemonic, "");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(Rules.EC_CURVE);

            BigInteger p = ecSpec.getCurve().getField().getCharacteristic();
            BigInteger d = new BigInteger(1, seed).remainder(p);
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

            namingClient.update(nodeName, getNodeUri(request), privateUpdatingKey, privateSigningKey, signingKey,
                    options);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        } catch (OperationFailure of) {
            throw new OperationFailure("node-name." + of.getErrorCode());
        }

        return Result.OK;
    }

    private static String getNodeUri(HttpServletRequest request) {
        return UriUtil.createBuilderFromRequest(request).replacePath("/moera").replaceQuery(null).toUriString();
    }

    @DeleteMapping
    @Admin(Scope.NAME)
    @Transactional
    public Result delete() {
        log.info("DELETE /node-name");

        if (requestContext.getOptions().getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        String prevNodeName = requestContext.getOptions().getString("profile.node-name");
        requestContext.getOptions().runInTransaction(options -> {
            options.reset("profile.node-name");
            options.reset("profile.signing-key");
        });
        requestContext.send(new NodeNameChangedLiberin(
                prevNodeName, requestContext.getOptions(), requestContext.getAvatar()));

        return Result.OK;
    }

    @PostMapping("/mnemonic")
    @Admin(Scope.NAME)
    @Transactional
    public Result postMnemonic(@Valid @RequestBody KeyMnemonic mnemonic) {
        log.info("POST /node-name/mnemonic");

        requestContext.getOptions().set("profile.updating-key.mnemonic", String.join(" ", mnemonic.getMnemonic()));

        requestContext.send(new NodeNameChangedLiberin(
                requestContext.nodeName(), requestContext.nodeName(), requestContext.getOptions(),
                requestContext.getAvatar()));

        return Result.OK;
    }

    @GetMapping("/mnemonic")
    @Admin(Scope.NAME)
    public KeyMnemonic getMnemonic() {
        log.info("GET /node-name/mnemonic");

        String mnemonic = requestContext.getOptions().getString("profile.updating-key.mnemonic");
        if (ObjectUtils.isEmpty(mnemonic)) {
            throw new ObjectNotFoundFailure("not-found");
        }

        return new KeyMnemonic(mnemonic.split(" "));
    }

    @DeleteMapping("/mnemonic")
    @Admin(Scope.NAME)
    @Transactional
    public Result deleteMnemonic() {
        log.info("DELETE /node-name/mnemonic");

        requestContext.getOptions().reset("profile.updating-key.mnemonic");

        requestContext.send(new NodeNameChangedLiberin(
                requestContext.nodeName(), requestContext.nodeName(), requestContext.getOptions(),
                requestContext.getAvatar()));

        return Result.OK;
    }

}
