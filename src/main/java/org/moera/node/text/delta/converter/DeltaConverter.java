package org.moera.node.text.delta.converter;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.text.delta.model.Delta;
import org.springframework.stereotype.Component;

@Component
public class DeltaConverter {

    @Inject
    private ObjectMapper objectMapper;

    public String toHtml(String source) {
        try {
            Delta delta = objectMapper.readValue(source, Delta.class);
            return toHtml(delta);
        } catch (JsonProcessingException e) {
            throw new BodyMappingException(e);
        }
    }

    private String toHtml(Delta source) {
        return TextSlice.parse(source).toHtml();
    }

}
