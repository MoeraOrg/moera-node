package org.moera.node.option.type;

import java.util.Arrays;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.option.exception.UnsuitableOptionValueException;

@OptionType("Principal")
public class PrincipalOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return ((Principal) value).getValue();
    }

    @Override
    public Object deserializeValue(String value) {
        return new Principal(value);
    }

    @Override
    public Principal getPrincipal(Object value) {
        return (Principal) value;
    }

    @Override
    public Object accept(Object value, Object typeModifiers) {
        if (value instanceof Principal) {
            return value;
        }
        if (value instanceof String) {
            return acceptString((String) value, (OptionTypeModifiers) typeModifiers);
        }
        return super.accept(value);
    }

    private Principal acceptString(String value, OptionTypeModifiers typeModifiers) {
        if (typeModifiers != null && typeModifiers.getPrincipals() != null
                && Arrays.stream(typeModifiers.getPrincipals()).noneMatch(v -> v.equals(value))) {
            throw new UnsuitableOptionValueException(value);
        }
        return new Principal(value);
    }

}
