package org.moera.node.model;

public class RegisteredNameSecret {

    private String[] mnemonic;
    private String secret;

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
