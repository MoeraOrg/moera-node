package org.moera.node.ui;

import org.moera.node.global.UiController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class RegistrarUiController {

    @GetMapping("/registrar")
    private String index(Model model) {
        return "registrar/index";
    }

}
