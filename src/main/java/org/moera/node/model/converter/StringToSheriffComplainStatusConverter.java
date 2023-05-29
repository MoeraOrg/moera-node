package org.moera.node.model.converter;

import org.moera.node.data.SheriffComplainStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSheriffComplainStatusConverter implements Converter<String, SheriffComplainStatus> {

    @Override
    public SheriffComplainStatus convert(String s) {
        return SheriffComplainStatus.parse(s);
    }

}
