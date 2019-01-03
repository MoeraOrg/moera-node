package org.moera.node.helper;

public class TypeMismatchException extends RuntimeException {

    private final String paramName;
    private final String typeName;
    private final Object value;

    public TypeMismatchException(String paramName, String typeName, Object value) {
        this.paramName = paramName;
        this.typeName = typeName;
        this.value = value;
    }

    public TypeMismatchException(String paramName, Class<?> cls, Object value) {
        this(paramName, cls.getName(), value);
    }

    public TypeMismatchException(String paramName, String typeName, Object value, Throwable cause) {
        super(cause);
        this.paramName = paramName;
        this.typeName = typeName;
        this.value = value;
    }

    public TypeMismatchException(String paramName, Class<?> cls, Object value, Throwable cause) {
        this(paramName, cls.getName(), value, cause);
    }

    public TypeMismatchException(int paramN, String typeName, Object value) {
        this(Integer.toString(paramN), typeName, value);
    }

    public TypeMismatchException(int paramN, Class<?> cls, Object value) {
        this(Integer.toString(paramN), cls, value);
    }

    public TypeMismatchException(int paramN, String typeName, Object value, Throwable cause) {
        this(Integer.toString(paramN), typeName, value, cause);
    }

    public TypeMismatchException(int paramN, Class<?> cls, Object value, Throwable cause) {
        this(Integer.toString(paramN), cls, value, cause);
    }

    public String getParamName() {
        return paramName;
    }

    public String getTypeName() {
        return typeName;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        return String.format("Incorrect value for type %s passed as parameter '%s': %s", typeName, paramName, value);
    }

}
