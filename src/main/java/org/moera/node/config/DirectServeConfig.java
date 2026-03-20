package org.moera.node.config;

public class DirectServeConfig {

    private DirectServeSource source = DirectServeSource.NONE;
    private String secret = "";

    public DirectServeSource getSource() {
        return source;
    }

    public void setSource(DirectServeSource source) {
        this.source = source;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

}
