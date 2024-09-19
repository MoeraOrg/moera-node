package org.moera.node.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyMnemonic {

    @NotNull
    private String[] mnemonic;

    public KeyMnemonic() {
    }

    public KeyMnemonic(String[] mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String[] getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String[] mnemonic) {
        this.mnemonic = mnemonic;
    }

}
