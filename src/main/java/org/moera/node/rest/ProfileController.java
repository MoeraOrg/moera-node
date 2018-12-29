package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.node.model.Profile;
import org.moera.node.option.Options;
import org.springframework.web.bind.annotation.GetMapping;
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
        Profile profile = new Profile();
        profile.setRegisteredName(options.getString("profile.registered-name"));
        profile.setRegisteredNameGeneration(options.getInt("profile.registered-name.generation"));
        profile.setSigningKeyDefined(options.getPrivateKey("profile.signing-key") != null);
        return profile;
    }

}
