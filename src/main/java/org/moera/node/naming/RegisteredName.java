package org.moera.node.naming;

import org.springframework.util.StringUtils;

public class RegisteredName implements NodeName {

    private String name;
    private Integer generation;

    public RegisteredName() {
    }

    public RegisteredName(String name, int generation) {
        this.name = name;
        this.generation = generation;
    }

    public static RegisteredName parse(String registeredName) {
        RegisteredName result = new RegisteredName();
        if (StringUtils.isEmpty(registeredName)) {
            return result;
        }
        String[] parts = registeredName.split("_");
        result.setName(parts[0]);
        if (parts.length > 1) {
            try {
                result.setGeneration(Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                throw new NodeNameParsingException("Invalid value for generation: " + parts[1]);
            }
        }
        return result;
    }

    public static String shorten(String registeredName) {
        return parse(registeredName).getName();
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
    public String toString() {
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
