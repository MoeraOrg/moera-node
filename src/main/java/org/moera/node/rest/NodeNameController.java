package org.moera.node.rest;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.moera.lib.Rules;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.KeyPair;
import org.moera.lib.crypto.MnemonicKey;
import org.moera.lib.node.types.KeyMnemonic;
import org.moera.lib.node.types.NameToRegister;
import org.moera.lib.node.types.NodeNameInfo;
import org.moera.lib.node.types.RegisteredNameSecret;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingClient;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.model.KeyMnemonicUtil;
import org.moera.node.model.NodeNameInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.option.Options;
import org.moera.node.util.UriUtil;
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

        return NodeNameInfoUtil.build(requestContext);
    }

    @PostMapping
    @Admin(Scope.NAME)
    @Transactional
    public RegisteredNameSecret post(@RequestBody NameToRegister nameToRegister, HttpServletRequest request) {
        log.info("POST /node-name (name = {})", LogUtil.format(nameToRegister.getName()));

        nameToRegister.validate();
        ValidationUtil.assertion(
            Rules.isNameValid(nameToRegister.getName()),
            "node-name.name.invalid"
        );

        Options options = requestContext.getOptions();
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }

        RegisteredNameSecret secretInfo = new RegisteredNameSecret();
        secretInfo.setName(nameToRegister.getName());
        MnemonicKey mnemonicKey = CryptoUtil.generateMnemonicKey();
        secretInfo.setSecret(mnemonicKey.getSecret());
        secretInfo.setMnemonic(Arrays.asList(mnemonicKey.getMnemonic().split(" ")));
        KeyPair signingKeyPair = CryptoUtil.generateKey();

        namingClient.register(
            nameToRegister.getName(),
            getNodeUri(request),
            mnemonicKey.getPublicKey(),
            signingKeyPair.getPrivateKey(),
            signingKeyPair.getPublicKey(),
            options
        );

        return secretInfo;
    }

    @PutMapping
    @Admin(Scope.NAME)
    @Transactional
    public Result put(@RequestBody RegisteredNameSecret registeredNameSecret, HttpServletRequest request) {
        log.info("PUT /node-name");

        Options options = requestContext.getOptions();
        if (options.getUuid("naming.operation.id") != null) {
            throw new OperationFailure("naming.operation-pending");
        }
        String nodeName = registeredNameSecret.getName() != null ? registeredNameSecret.getName() : options.nodeName();
        if (ObjectUtils.isEmpty(nodeName)) {
            throw new ValidationFailure("node-name.name-absent");
        }
        if ((registeredNameSecret.getMnemonic() == null || registeredNameSecret.getMnemonic().isEmpty())
                && ObjectUtils.isEmpty(registeredNameSecret.getSecret())) {
            throw new ValidationFailure("registeredNameSecret.empty");
        }

        String mnemonic = !ObjectUtils.isEmpty(registeredNameSecret.getSecret())
            ? CryptoUtil.secretToMnemonic(registeredNameSecret.getSecret())
            : String.join(" ", registeredNameSecret.getMnemonic());
        ECPrivateKey privateUpdatingKey = CryptoUtil.mnemonicToPrivateKey(mnemonic);

        ECPrivateKey privateSigningKey = null;
        ECPublicKey signingKey = null;
        if (registeredNameSecret.getName() != null) {
            KeyPair signingKeyPair = CryptoUtil.generateKey();
            privateSigningKey = signingKeyPair.getPrivateKey();
            signingKey = signingKeyPair.getPublicKey();
        }

        try {
            namingClient.update(
                nodeName, getNodeUri(request), privateUpdatingKey, privateSigningKey, signingKey, options
            );
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
    public Result postMnemonic(@RequestBody KeyMnemonic mnemonic) {
        log.info("POST /node-name/mnemonic");

        mnemonic.validate();
        requestContext.getOptions().set("profile.updating-key.mnemonic", String.join(" ", mnemonic.getMnemonic()));

        requestContext.send(
            new NodeNameChangedLiberin(
                requestContext.nodeName(),
                requestContext.nodeName(),
                requestContext.getOptions(),
                requestContext.getAvatar()
            )
        );

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

        return KeyMnemonicUtil.build(Arrays.asList(mnemonic.split(" ")));
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
