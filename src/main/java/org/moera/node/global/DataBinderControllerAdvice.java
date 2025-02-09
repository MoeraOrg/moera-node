package org.moera.node.global;

import jakarta.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class DataBinderControllerAdvice {

    @Inject
    private ApplicationContext applicationContext;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        DefaultMessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
        messageCodesResolver.setMessageCodeFormatter(new SmartMessageCodeFormatter());
        binder.setMessageCodesResolver(messageCodesResolver);
    }

}
