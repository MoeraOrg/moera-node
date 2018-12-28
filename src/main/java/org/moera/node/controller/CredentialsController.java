package org.moera.node.controller;

import java.security.NoSuchAlgorithmException;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.Password;
import org.moera.node.model.Credentials;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
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
    public Result post(@Valid @RequestBody Credentials credentials) throws NoSuchAlgorithmException {
        options.set("credentials.login", credentials.getLogin());
        options.set("credentials.password-hash", Password.hash(credentials.getPassword()));

        return Result.OK;
    }

}
