package org.moera.node.model.converter;

import org.moera.lib.node.types.StoryType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StoryTypeToStringConverter implements Converter<StoryType, String> {

    @Override
    public String convert(StoryType storyType) {
        return storyType.getValue();
    }

}
