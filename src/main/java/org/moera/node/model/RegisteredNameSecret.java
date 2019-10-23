package org.moera.node.model;

public class RegisteredNameSecret {

    private String name;
    private String[] mnemonic;
    private String secret;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String[] mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

}
