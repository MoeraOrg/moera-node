package org.moera.node.rest;

import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.Password;
import org.moera.node.global.Admin;
import org.moera.node.model.Credentials;
import org.moera.node.model.CredentialsCreated;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera-node/credentials")
public class CredentialsController {

    @Inject
    private Options options;

    @GetMapping
    @ResponseBody
    public CredentialsCreated get() {
        return new CredentialsCreated(
                !StringUtils.isEmpty(options.getString("credentials.login"))
                && !StringUtils.isEmpty(options.getString("credentials.password-hash")));
    }

    @PostMapping
    @ResponseBody
    public Result post(@Valid @RequestBody Credentials credentials) {
        if (!StringUtils.isEmpty(options.getString("credentials.login"))
                && !StringUtils.isEmpty(options.getString("credentials.password-hash"))) {
            throw new OperationFailure("credentials.already-created");
        }
        options.set("credentials.login", credentials.getLogin());
        options.set("credentials.password-hash", Password.hash(credentials.getPassword()));

        return Result.OK;
    }

    @PutMapping
    @Admin
    @ResponseBody
    public Result put(@Valid @RequestBody Credentials credentials) {
        options.set("credentials.login", credentials.getLogin());
        options.set("credentials.password-hash", Password.hash(credentials.getPassword()));

        return Result.OK;
    }

}
