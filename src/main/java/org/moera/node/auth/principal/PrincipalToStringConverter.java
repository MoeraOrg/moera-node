package org.moera.node.auth.principal;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PrincipalToStringConverter implements Converter<Principal, String> {

    @Override
    public String convert(Principal source) {
        return source.getValue();
    }

}
