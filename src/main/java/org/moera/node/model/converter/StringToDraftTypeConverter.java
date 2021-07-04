package org.moera.node.model.converter;

import org.moera.node.data.DraftType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDraftTypeConverter implements Converter<String, DraftType> {

    @Override
    public DraftType convert(String s) {
        return DraftType.parse(s);
    }

}
