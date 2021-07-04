package org.moera.node.model.converter;

import org.moera.node.data.DraftType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DraftTypeToStringConverter implements Converter<DraftType, String> {

    @Override
    public String convert(DraftType draftType) {
        return draftType.getValue();
    }

}
