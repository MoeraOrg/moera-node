package org.moera.node.model.converter;

import org.moera.lib.node.types.StoryType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToStoryTypeConverter implements Converter<String, StoryType> {

    @Override
    public StoryType convert(String s) {
        return StoryType.parse(s);
    }

}
