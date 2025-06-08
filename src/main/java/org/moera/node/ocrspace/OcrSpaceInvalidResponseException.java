package org.moera.node.ocrspace;

public class OcrSpaceInvalidResponseException extends OcrSpaceException {

    public OcrSpaceInvalidResponseException() {
        super("Invalid response from OCR.space service");
    }

    public OcrSpaceInvalidResponseException(Throwable cause) {
        super("Invalid response from OCR.space service", cause);
    }

}
