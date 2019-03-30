package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import org.moera.node.option.Options;

public class RegisteredNameInfo {

    private String name;
    private Integer generation;
    private Map<String, String[]> operations;

    public RegisteredNameInfo() {
    }

    public RegisteredNameInfo(Options options) {
        name = options.getString("profile.registered-name");
        generation = options.getInt("profile.registered-name.generation");
        operations = Collections.singletonMap("manage", new String[]{"admin"});
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

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
