package org.moera.node.model.body;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.util.ObjectUtils;

@JsonSerialize(converter = Body.ToStringConverter.class)
@JsonDeserialize(converter = Body.FromStringConverter.class)
public class Body implements Cloneable {

    public static final String EMPTY = "{}";

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
        getLinkPreviews().forEach(lp -> lp.setParent(this));
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

    public String getSubject() {
        return decoded.getSubject();
    }

    public void setSubject(String subject) {
        decoded.setSubject(subject);
        modified();
    }

    public String getText() {
        return decoded.getText();
    }

    public void setText(String text) {
        decoded.setText(text);
        modified();
    }

    public List<LinkPreview> getLinkPreviews() {
        return decoded.getLinkPreviews() != null
                ? Collections.unmodifiableList(decoded.getLinkPreviews())
                : Collections.emptyList();
    }

    public void setLinkPreviews(List<LinkPreview> linkPreviews) {
        linkPreviews.forEach(lp -> lp.setParent(this));
        decoded.setLinkPreviews(linkPreviews);
        modified();
    }

    void modified() {
        encoded = null;
    }

    public String getAllText() {
        StringBuilder buf = new StringBuilder(getText());
        for (LinkPreview linkPreview : getLinkPreviews()) {
            if (!ObjectUtils.isEmpty(linkPreview.getTitle())) {
                buf.append(' ');
                buf.append(linkPreview.getTitle());
            }
            if (!ObjectUtils.isEmpty(linkPreview.getDescription())) {
                buf.append(' ');
                buf.append(linkPreview.getDescription());
            }
        }
        return buf.toString();
    }

    @Override
    public Body clone() {
        return new Body(getEncoded());
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
