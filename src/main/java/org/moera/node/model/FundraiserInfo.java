package org.moera.node.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class FundraiserInfo {

    private static final Logger log = LoggerFactory.getLogger(FundraiserInfo.class);

    private final Map<String, String> fields = new HashMap<>();

    public static String serializeValue(FundraiserInfo[] fundraisers) {
        if (fundraisers == null) {
            return null;
        } else if (ObjectUtils.isEmpty(fundraisers)) {
            return "";
        } else {
            try {
                return new ObjectMapper().writeValueAsString(fundraisers);
            } catch (JsonProcessingException e) {
                log.error("Error serializing FundraiserInfo[]", e);
                return null;
            }
        }
    }

    public static FundraiserInfo[] deserializeValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(value, FundraiserInfo[].class);
        } catch (IOException e) {
            throw new DeserializeOptionValueException("FundraiserInfo[]", value);
        }
    }

    @JsonAnyGetter
    public Map<String, String> getFields() {
        return fields;
    }

    @JsonAnySetter
    public void setField(String name, String value) {
        fields.put(name, value);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
