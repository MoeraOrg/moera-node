package org.moera.node.auth;

public record AuthSecrets(
    String rootSecret,
    String token,
    String carte
) {

    public AuthSecrets() {
        this(null);
    }

    public AuthSecrets(String auth) {
        this(rootSecret(auth), token(auth), carte(auth));
    }

    private static String rootSecret(String auth) {
        return auth != null && auth.startsWith("secret:") ? auth.substring(7) : null;
    }

    private static String token(String auth) {
        if (auth == null || auth.startsWith("secret:") || auth.startsWith("carte:")) {
            return null;
        }
        return auth.startsWith("token:") ? auth.substring(6) : auth;
    }

    private static String carte(String auth) {
        return auth != null && auth.startsWith("carte:") ? auth.substring(6) : null;
    }

}
