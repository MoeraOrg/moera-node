package org.moera.node.model.converter;

import org.moera.lib.node.types.SheriffComplaintStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SheriffComplaintStatusToStringConverter implements Converter<SheriffComplaintStatus, String> {

    @Override
    public String convert(SheriffComplaintStatus status) {
        return status.getValue();
    }

}
