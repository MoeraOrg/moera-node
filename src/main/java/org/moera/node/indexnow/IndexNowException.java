package org.moera.node.indexnow;

public class IndexNowException extends Exception {

    private final String host;

    public IndexNowException(String host, String message) {
        super(message + String.format(" (host: %s)", host));
        this.host = host;
    }

    public IndexNowException(String host, String message, Throwable cause) {
        super(message + String.format(" (host: %s)", host), cause);
        this.host = host;
    }

    public String getHost() {
        return host;
    }

}
