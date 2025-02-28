package org.moera.node.model.converter;

import org.moera.lib.node.types.SearchEngine;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SearchEngineToStringConverter implements Converter<SearchEngine, String> {

    @Override
    public String convert(SearchEngine searchEngine) {
        return searchEngine.getValue();
    }

}
