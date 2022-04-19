package org.moera.node.auth;

public class AuthSecrets {

    public String rootSecret;
    public String token;
    public String carte;

    public AuthSecrets() {
    }

    public AuthSecrets(String auth) {
        if (auth != null) {
            if (auth.startsWith("secret:")) {
                rootSecret = auth.substring(7);
            } else if (auth.startsWith("token:")) {
                token = auth.substring(6);
            } else if (auth.startsWith("carte:")) {
                carte = auth.substring(6);
            } else {
                token = auth;
            }
        }
    }

}
