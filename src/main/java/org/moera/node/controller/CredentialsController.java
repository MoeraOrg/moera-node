package org.moera.node.controller;

import java.security.NoSuchAlgorithmException;
import javax.inject.Inject;

import org.moera.commons.util.Password;
import org.moera.node.model.Credentials;
import org.moera.node.model.Success;
import org.moera.node.option.Options;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera-node/credentials")
public class CredentialsController {

    @Inject
    private Options options;

    @PostMapping
    @ResponseBody
    public Success post(@RequestBody Credentials credentials) throws NoSuchAlgorithmException {
        if (StringUtils.isEmpty(credentials.getLogin())) {
            return new Success(1, "login is empty");
        }
        if (StringUtils.isEmpty(credentials.getPassword())) {
            return new Success(2, "password is empty");
        }
        options.set("credentials.login", credentials.getLogin());
        options.set("credentials.password-hash", Password.hash(credentials.getPassword()));

        return Success.OK;
    }

}
