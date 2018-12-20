package org.moera.node.controller;

import org.moera.node.model.RegisteredName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @PostMapping("/moera-node/registered-name")
    public void post(@RequestBody RegisteredName registeredName) {
        log.info("Asked to register the name '{}'", registeredName.getName());
    }

}
