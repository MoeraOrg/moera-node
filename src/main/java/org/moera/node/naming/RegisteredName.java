package org.moera.node.naming;

import org.springframework.util.StringUtils;

public class RegisteredName implements NodeName {

    private String name;
    private int generation;

    public RegisteredName() {
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
        if (StringUtils.isEmpty(registeredName)) {
            return registeredName;
        }
        RegisteredName rn = parse(registeredName);
        return rn.getGeneration() != 0 ? rn.toString() : rn.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    @Override
    public String toString() {
        return toString(name, generation);
    }

    public static String toString(String name, int generation) {
        return name != null ? String.format("%s_%d", name, generation) : null;
    }

}
