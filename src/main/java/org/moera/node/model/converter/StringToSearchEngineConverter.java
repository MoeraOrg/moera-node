package org.moera.node.model.converter;

import org.moera.lib.node.types.SearchEngine;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSearchEngineConverter implements Converter<String, SearchEngine> {

    @Override
    public SearchEngine convert(String s) {
        return SearchEngine.parse(s);
    }

}
