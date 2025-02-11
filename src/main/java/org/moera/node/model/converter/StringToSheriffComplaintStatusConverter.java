package org.moera.node.model.converter;

import org.moera.lib.node.types.SheriffComplaintStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSheriffComplaintStatusConverter implements Converter<String, SheriffComplaintStatus> {

    @Override
    public SheriffComplaintStatus convert(String s) {
        return SheriffComplaintStatus.parse(s);
    }

}
