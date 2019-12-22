package org.moera.node.auth;

public class IncorrectSignatureException extends RuntimeException {

    public IncorrectSignatureException() {
        super("Signature is incorrect");
    }

}
