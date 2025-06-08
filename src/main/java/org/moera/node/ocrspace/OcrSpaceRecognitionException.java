package org.moera.node.ocrspace;

import org.springframework.util.ObjectUtils;

public class OcrSpaceRecognitionException extends OcrSpaceException {

    private int ocrExitCode;
    private boolean isErroredOnProcessing;
    private String ocrErrorMessage;
    private String ocrErrorDetails;
    private Integer fileExitCode;
    private String fileErrorMessage;
    private String fileErrorDetails;

    public OcrSpaceRecognitionException(OcrResult result) {
        this(result, null);
    }

    public OcrSpaceRecognitionException(OcrResult result, Throwable cause) {
        super(getMessage(result), cause);

        this.ocrExitCode = result.getOcrExitCode();
        this.isErroredOnProcessing = result.isErroredOnProcessing();
        this.ocrErrorMessage = result.getErrorMessage();
        this.ocrErrorDetails = result.getErrorDetails();
        if (!ObjectUtils.isEmpty(result.getParsedResults())) {
            this.fileExitCode = result.getParsedResults().get(0).getFileParseExitCode();
            this.fileErrorMessage = result.getParsedResults().get(0).getErrorMessage();
            this.fileErrorDetails = result.getParsedResults().get(0).getErrorDetails();
        }
    }

    private static String getMessage(OcrResult result) {
        String message = "Error during OCR.space recognition";
        if (!ObjectUtils.isEmpty(result.getErrorMessage())) {
            message += ": " + result.getErrorMessage();
        }
        if (!ObjectUtils.isEmpty(result.getParsedResults())) {
            String errorMessage = result.getParsedResults().get(0).getErrorMessage();
            if (!ObjectUtils.isEmpty(errorMessage)) {
                message += " - " + errorMessage;
            }
        }
        return message;
    }

    public int getOcrExitCode() {
        return ocrExitCode;
    }

    public void setOcrExitCode(int ocrExitCode) {
        this.ocrExitCode = ocrExitCode;
    }

    public boolean isErroredOnProcessing() {
        return isErroredOnProcessing;
    }

    public void setErroredOnProcessing(boolean erroredOnProcessing) {
        isErroredOnProcessing = erroredOnProcessing;
    }

    public String getOcrErrorMessage() {
        return ocrErrorMessage;
    }

    public void setOcrErrorMessage(String ocrErrorMessage) {
        this.ocrErrorMessage = ocrErrorMessage;
    }

    public String getOcrErrorDetails() {
        return ocrErrorDetails;
    }

    public void setOcrErrorDetails(String ocrErrorDetails) {
        this.ocrErrorDetails = ocrErrorDetails;
    }

    public Integer getFileExitCode() {
        return fileExitCode;
    }

    public void setFileExitCode(Integer fileExitCode) {
        this.fileExitCode = fileExitCode;
    }

    public String getFileErrorMessage() {
        return fileErrorMessage;
    }

    public void setFileErrorMessage(String fileErrorMessage) {
        this.fileErrorMessage = fileErrorMessage;
    }

    public String getFileErrorDetails() {
        return fileErrorDetails;
    }

    public void setFileErrorDetails(String fileErrorDetails) {
        this.fileErrorDetails = fileErrorDetails;
    }

}
