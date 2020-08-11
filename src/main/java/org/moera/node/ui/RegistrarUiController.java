package org.moera.node.ui;

import org.moera.node.global.UiController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class RegistrarUiController {

    @Value("${registrar.domain}")
    private String registrarDomain;

    @GetMapping("/registrar")
    private String index(Model model) {
        model.addAttribute("registrarDomain", registrarDomain);

        return "registrar/index";
    }

}
