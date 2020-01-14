package org.moera.node.model;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.util.EmojiList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptedReactions {

    @Size(max = 255)
    @Pattern(regexp = EmojiList.PATTERN)
    private String positive;

    @Size(max = 255)
    @Pattern(regexp = EmojiList.PATTERN)
    private String negative;

    public String getPositive() {
        return positive;
    }

    public void setPositive(String positive) {
        this.positive = positive;
    }

    public String getNegative() {
        return negative;
    }

    public void setNegative(String negative) {
        this.negative = negative;
    }

}
