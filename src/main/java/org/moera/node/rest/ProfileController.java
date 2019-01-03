package org.moera.node.rest;

import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.node.global.Admin;
import org.moera.node.model.Profile;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moera-node/profile")
public class ProfileController {

    @Inject
    private Options options;

    @GetMapping
    @ResponseBody
    public Profile get() {
        return new Profile(options);
    }

    @PutMapping
    @Admin
    @ResponseBody
    public Result put(@Valid @RequestBody Profile profile) {
        profile.toOptions(options);
        return Result.OK;
    }

}
