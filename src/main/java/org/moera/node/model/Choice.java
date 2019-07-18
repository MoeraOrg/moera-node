package org.moera.node.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.data.Choosable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Choice {

    private static Logger log = LoggerFactory.getLogger(Choice.class);

    private String value;
    private String title;

    public Choice() {
    }

    private Choice(Choosable c) {
        value = c.getValue();
        title = c.getTitle();
    }

    public static List<Choice> forEnum(Class<? extends Enum<? extends Choosable>> e) {
        try {
            return Arrays.stream((Choosable[]) e.getMethod("values").invoke(null))
                    .map(Choice::new)
                    .collect(Collectors.toList());
        } catch (ReflectiveOperationException ex) {
            log.error("Error creating list of constants for {} enum: {}", e.getCanonicalName(), ex.getMessage());
            return null;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
