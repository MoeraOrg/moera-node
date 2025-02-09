package org.moera.node.auth.principal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PrincipalAttributeConverter implements AttributeConverter<Principal, String> {

    @Override
    public String convertToDatabaseColumn(Principal principal) {
        return principal != null ? principal.getValue() : null;
    }

    @Override
    public Principal convertToEntityAttribute(String s) {
        return s != null ? new Principal(s) : null;
    }

}
