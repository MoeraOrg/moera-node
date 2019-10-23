package org.moera.node.naming;

import org.springframework.util.StringUtils;

public class DelegatedName implements RegisteredName {

    private String name;
    private Integer generation;

    public DelegatedName() {
    }

    public DelegatedName(String name, int generation) {
        this.name = name;
        this.generation = generation;
    }

    public static DelegatedName parse(String delegatedName) {
        DelegatedName result = new DelegatedName();
        if (StringUtils.isEmpty(delegatedName)) {
            return result;
        }
        String[] parts = delegatedName.split("_");
        result.setName(parts[0]);
        if (parts.length > 1) {
            try {
                result.setGeneration(Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                throw new RegisteredNameParsingException("Invalid value for generation: " + parts[1]);
            }
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    @Override
    public String format() {
        if (name != null) {
            if (generation != null) {
                return String.format("%s_%d", name, generation);
            } else {
                return name;
            }
        }
        return null;
    }

}
