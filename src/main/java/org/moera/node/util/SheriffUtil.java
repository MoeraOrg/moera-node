package org.moera.node.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.Entry;
import org.moera.node.data.SheriffMark;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
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

    public static void updateSheriffMarks(String sheriffName, boolean isDelete,
                                          Supplier<String> getter, Consumer<String> setter) {
        List<SheriffMark> sheriffMarks = SheriffUtil.deserializeSheriffMarks(getter.get())
                .orElse(Collections.emptyList())
                .stream()
                .filter(mark -> !mark.getSheriffName().equals(sheriffName))
                .collect(Collectors.toList());
        if (!isDelete) {
            sheriffMarks.add(new SheriffMark(sheriffName));
        }
        setter.accept(SheriffUtil.serializeSheriffMarks(sheriffMarks).orElse(""));
    }

    public static void updateSheriffMarks(String sheriffName, boolean isDelete, Entry entry) {
        updateSheriffMarks(sheriffName, isDelete, entry::getSheriffMarks, entry::setSheriffMarks);
    }

    public static List<SheriffMark> concatSheriffMark(List<SheriffMark> sheriffMarks, String sheriffName) {
        if (sheriffMarks != null && sheriffMarks.stream().anyMatch(sm -> sm.getSheriffName().equals(sheriffName))) {
            return sheriffMarks;
        }
        List<SheriffMark> allMarks = new ArrayList<>(sheriffMarks != null ? sheriffMarks : Collections.emptyList());
        allMarks.add(new SheriffMark(sheriffName));
        return allMarks;
    }

    public static void addSheriffMark(PostingInfo postingInfo, String sheriffName) {
        postingInfo.setSheriffMarks(SheriffUtil.concatSheriffMark(postingInfo.getSheriffMarks(), sheriffName));
    }

    public static void addSheriffMark(CommentInfo commentInfo, String sheriffName) {
        commentInfo.setSheriffMarks(SheriffUtil.concatSheriffMark(commentInfo.getSheriffMarks(), sheriffName));
    }

}
