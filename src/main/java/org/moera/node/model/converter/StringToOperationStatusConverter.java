package org.moera.node.model.converter;

import org.moera.lib.node.types.OperationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToOperationStatusConverter implements Converter<String, OperationStatus> {

    @Override
    public OperationStatus convert(String s) {
        return OperationStatus.parse(s);
    }

}
