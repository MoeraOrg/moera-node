package org.moera.node.model.converter;

import org.moera.lib.node.types.SubscriptionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionTypeToStringConverter implements Converter<SubscriptionType, String> {

    @Override
    public String convert(SubscriptionType subscriptionType) {
        return subscriptionType.getValue();
    }

}
