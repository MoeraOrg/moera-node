package org.moera.node.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.SheriffMark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class SheriffUtil {

    private static final Logger log = LoggerFactory.getLogger(SheriffUtil.class);

    public static Optional<List<String>> deserializeSheriffs(String sheriffs) {
        if (ObjectUtils.isEmpty(sheriffs)) {
            return Optional.empty();
        }
        return Optional.of(
                Arrays.stream(sheriffs.split(","))
                    .map(String::strip)
                    .collect(Collectors.toList()));
    }

    public static Optional<String> serializeSheriffs(List<String> sheriffs) {
        if (ObjectUtils.isEmpty(sheriffs)) {
            return Optional.empty();
        }
        return Optional.of(String.join(",", sheriffs));
    }

    public static Optional<List<SheriffMark>> deserializeSheriffMarks(String sheriffMarks) {
        if (!ObjectUtils.isEmpty(sheriffMarks)) {
            try {
                return Optional.of(Arrays.asList(new ObjectMapper().readValue(sheriffMarks, SheriffMark[].class)));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing sheriff marks", e);
            }
        }
        return Optional.empty();
    }

    public static Optional<String> serializeSheriffMarks(List<SheriffMark> sheriffMarks) {
        if (!ObjectUtils.isEmpty(sheriffMarks)) {
            try {
                return Optional.of(new ObjectMapper().writeValueAsString(sheriffMarks));
            } catch (JsonProcessingException e) {
                log.error("Error serializing sheriff marks", e);
            }
        }
        return Optional.empty();
    }

}
