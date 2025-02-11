package org.moera.node.model.converter;

import org.moera.lib.node.types.SubscriptionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSubscriptionTypeConverter implements Converter<String, SubscriptionType> {

    @Override
    public SubscriptionType convert(String s) {
        return SubscriptionType.parse(s);
    }

}
