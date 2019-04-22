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
import org.moera.commons.util.CryptoException;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.Rules;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.NameToRegister;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RegisteredNameInfo;
import org.moera.node.model.RegisteredNameSecret;
import org.moera.node.model.Result;
import org.moera.node.naming.NamingClient;
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
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/registered-name")
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Options options;

    @Inject
    private NamingClient namingClient;

    @GetMapping
    @ResponseBody
    public RegisteredNameInfo get() {
        log.info("GET /registered-name");

        return new RegisteredNameInfo(options, requestContext);
    }

    @PostMapping
    @Admin
    @ResponseBody
    public RegisteredNameSecret post(@Valid @RequestBody NameToRegister nameToRegister, HttpServletRequest request) {
        if (!Rules.isNameValid(nameToRegister.getName())) {
            throw new OperationFailure("nameToRegister.name.invalid");
        }

        log.info("POST /registered-name (name = '{}')", nameToRegister.getName());

        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        RegisteredNameSecret secretInfo = new RegisteredNameSecret();
        KeyPair signingKeyPair;
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();

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
                    (ECPublicKey) signingKeyPair.getPublic());
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
        options.set("profile.signing-key", signingKeyPair.getPrivate());

        return secretInfo;
    }

    @PutMapping
    @Admin
    @ResponseBody
    public Result put(@Valid @RequestBody RegisteredNameSecret registeredNameSecret) {
        log.info("PUT /registered-name");

        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        String name = options.getString("profile.registered-name");
        Integer generation = options.getInt("profile.registered-name.generation");
        if (StringUtils.isEmpty(name) || generation == null) {
            throw new OperationFailure("registered-name.name-absent");
        }
        if ((registeredNameSecret.getMnemonic() == null || registeredNameSecret.getMnemonic().length == 0)
                && StringUtils.isEmpty(registeredNameSecret.getSecret())) {
            throw new OperationFailure("registeredNameSecret.empty");
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
        byte[] seed = new SeedCalculator(JavaxPBKDF2WithHmacSHA512.INSTANCE).calculateSeed(mnemonic.toString(), "");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

            BigInteger d = new BigInteger(seed);
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(d, ecSpec);
            ECPrivateKey privateUpdatingKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            namingClient.update(name, generation, privateUpdatingKey);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        } catch (OperationFailure of) {
            throw new OperationFailure("registered-name." + of.getErrorCode());
        }

        return Result.OK;
    }

    @DeleteMapping
    @Admin
    @ResponseBody
    public Result delete() {
        log.info("DELETE /registered-name");

        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        options.reset("profile.registered-name");
        options.reset("profile.registered-name.generation");
        options.reset("profile.signing-key");

        return Result.OK;
    }

}
