package org.moera.node.model.converter;

import org.moera.node.data.SheriffComplainStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SheriffComplainStatusToStringConverter implements Converter<SheriffComplainStatus, String> {

    @Override
    public String convert(SheriffComplainStatus status) {
        return status.getValue();
    }

}
