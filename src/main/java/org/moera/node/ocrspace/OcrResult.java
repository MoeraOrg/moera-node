package org.moera.node.ocrspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrResult {

    @JsonProperty("ParsedResults")
    private List<ParsedResult> parsedResults;

    @JsonProperty("OCRExitCode")
    private int ocrExitCode;

    @JsonProperty("IsErroredOnProcessing")
    private boolean isErroredOnProcessing;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ErrorDetails")
    private String errorDetails;

    public List<ParsedResult> getParsedResults() {
        return parsedResults;
    }

    public void setParsedResults(List<ParsedResult> parsedResults) {
        this.parsedResults = parsedResults;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

}
