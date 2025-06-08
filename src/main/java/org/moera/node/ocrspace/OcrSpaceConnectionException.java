package org.moera.node.ocrspace;

public class OcrSpaceConnectionException extends OcrSpaceException {

    public OcrSpaceConnectionException() {
        super("Error connecting OCR.space service");
    }

    public OcrSpaceConnectionException(Throwable cause) {
        super("Error connecting OCR.space service", cause);
    }

}
