package org.moera.node.rest;

import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.Password;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.model.Credentials;
import org.moera.node.model.CredentialsCreated;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/credentials")
public class CredentialsController {

    private static Logger log = LoggerFactory.getLogger(CredentialsController.class);

    @Inject
    private Options options;

    @GetMapping
    @ResponseBody
    public CredentialsCreated get() {
        log.info("GET /credentials");

        return new CredentialsCreated(
                !StringUtils.isEmpty(options.getString("credentials.login"))
                && !StringUtils.isEmpty(options.getString("credentials.password-hash")));
    }

    @PostMapping
    @ResponseBody
    public Result post(@Valid @RequestBody Credentials credentials) {
        log.info("POST /credentials (login = '{}')", credentials.getLogin());

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
        log.info("PUT /credentials (login = '{}')", credentials.getLogin());

        options.set("credentials.login", credentials.getLogin());
        options.set("credentials.password-hash", Password.hash(credentials.getPassword()));

        return Result.OK;
    }

}
