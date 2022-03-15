package org.moera.node.model.body;

public class BodyMappingException extends RuntimeException {

    private String field;

    public BodyMappingException() {
        super("Error mapping a Body object");
    }

    public BodyMappingException(Throwable cause) {
        super("Error mapping a Body object", cause);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Override
    public String getMessage() {
        return field != null ? super.getMessage() + ": " + field : super.getMessage();
    }

}
