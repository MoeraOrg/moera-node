package org.moera.node.global;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Inject
    private MessageSource messageSource;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> ea = super.getErrorAttributes(webRequest, options);
        if (Objects.equals(ea.get("status"), 404) || Objects.equals(ea.get("status"), 405)) {
            Map<String, Object> errorAttributes = new HashMap<>();
            String errorCode = "not-found";
            String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
            errorAttributes.put("errorCode", errorCode);
            errorAttributes.put("message", message);
            return errorAttributes;
        }
        return ea;
    }

}
