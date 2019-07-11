package org.moera.node.global;

import javax.inject.Inject;

import org.moera.node.model.RegisteredNameInfo;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalsControllerAdvice {

    @Inject
    private RequestContext requestContext;

    @ModelAttribute
    public void session(Model model) {
        model.addAttribute("registeredName", new RegisteredNameInfo(requestContext.getPublic()));
    }

}
