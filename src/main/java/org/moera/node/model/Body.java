package org.moera.node.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonSerialize(converter = Body.ToStringConverter.class)
@JsonDeserialize(converter = Body.FromStringConverter.class)
public class Body {

    private String encoded;
    private BodyDecoded decoded = new BodyDecoded();

    public Body() {
    }

    public Body(String encoded) {
        setEncoded(encoded);
    }

    private void decode() {
        if (encoded == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            decoded = mapper.readValue(encoded, BodyDecoded.class);
        } catch (IOException e) {
            throw new BodyMappingException(e);
        }
    }

    private void encode() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            encoded = mapper.writeValueAsString(decoded);
        } catch (JsonProcessingException e) {
            throw new BodyMappingException(e);
        }
    }

    public String getEncoded() {
        if (encoded == null) {
            encode();
        }
        return encoded;
    }

    public void setEncoded(String encoded) {
        this.encoded = encoded;
        decode();
    }

    public String getText() {
        return decoded.getText();
    }

    public void setText(String text) {
        decoded.setText(text);
        encoded = null;
    }

    public static class ToStringConverter extends StdConverter<Body, String> {

        @Override
        public String convert(Body body) {
            return body.getEncoded();
        }

    }

    public static class FromStringConverter extends StdConverter<String, Body> {

        @Override
        public Body convert(String s) {
            return new Body(s);
        }

    }

}
