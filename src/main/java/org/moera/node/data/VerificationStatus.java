package org.moera.node.data;

public enum VerificationStatus {

    RUNNING,
    CORRECT,
    INCORRECT,
    ERROR;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

}
