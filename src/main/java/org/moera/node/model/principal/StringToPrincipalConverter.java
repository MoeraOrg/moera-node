package org.moera.node.model.principal;

import org.moera.lib.node.types.principal.Principal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToPrincipalConverter implements Converter<String, Principal> {

    @Override
    public Principal convert(String source) {
        return new Principal(source);
    }

}
