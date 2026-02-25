package org.moera.node.ui;

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class UiErrorViewResolver implements ErrorViewResolver {

    @Inject
    @Lazy
    private TitleBuilder titleBuilder;

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        Map<String, Object> errorModel = new HashMap<>(model);
        errorModel.put("status", status.value());
        errorModel.put("comment", status.getReasonPhrase());
        errorModel.put("pageTitle", titleBuilder.build(status.value() + " - " + status.getReasonPhrase()));
        return new ModelAndView("fail", errorModel);
    }

}
