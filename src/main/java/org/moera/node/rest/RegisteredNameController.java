package org.moera.node.rest;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.inject.Inject;

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
import org.moera.node.global.Admin;
import org.moera.node.model.NameToRegister;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RegisteredNameSecret;
import org.moera.node.model.Result;
import org.moera.node.naming.NamingClient;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera/api/registered-name")
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @Inject
    private Options options;

    @Inject
    private NamingClient namingClient;

    @PostMapping
    @Admin
    @ResponseBody
    public RegisteredNameSecret post(@RequestBody NameToRegister nameToRegister) {
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("nameToRegister.operation-pending");
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
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

            BigInteger d = new BigInteger(seed);
            ECPoint q = ecSpec.getG().multiply(d);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(q, ecSpec);
            PublicKey publicUpdatingKey = keyFactory.generatePublic(pubSpec);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, random);
            signingKeyPair = keyPairGenerator.generateKeyPair();

            namingClient.register(nameToRegister.getName(), publicUpdatingKey, signingKeyPair.getPublic());
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
        options.set("profile.signing-key", signingKeyPair.getPrivate());

        return secretInfo;
    }

    @PutMapping
    @Admin
    @ResponseBody
    public Result put(@RequestBody RegisteredNameSecret registeredNameSecret) {
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("registeredNameSecret.operation-pending");
        }
        String name = options.getString("profile.registered-name");
        Integer generation = options.getInt("profile.registered-name.generation");
        if (StringUtils.isEmpty(name) || generation == null) {
            throw new OperationFailure("registeredNameSecret.registered-name-absent");
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
            PrivateKey privateUpdatingKey = keyFactory.generatePrivate(privateKeySpec);

            namingClient.update(name, generation, privateUpdatingKey);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        } catch (OperationFailure of) {
            throw new OperationFailure("registeredNameSecret." + of.getErrorCode());
        }

        return Result.OK;
    }

}
