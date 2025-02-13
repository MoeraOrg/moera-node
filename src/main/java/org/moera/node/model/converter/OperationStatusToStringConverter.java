package org.moera.node.model.converter;

import org.moera.lib.node.types.OperationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class OperationStatusToStringConverter implements Converter<OperationStatus, String> {

    @Override
    public String convert(OperationStatus operationStatus) {
        return operationStatus.getValue();
    }

}
