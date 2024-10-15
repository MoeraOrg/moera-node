package org.moera.node.text.delta.model;

import java.util.Arrays;
import java.util.List;

public record LineSeparator(String newLine, List<String> embeds) {

    public LineSeparator(String newLine, String... embeds) {
        this(newLine, Arrays.asList(embeds));
    }

}
