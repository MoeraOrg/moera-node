package org.moera.node.global;

import org.springframework.lang.NonNull;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodeFormatter;

public class SmartMessageCodeFormatter implements MessageCodeFormatter {

    @Override
    public String format(@NonNull String errorCode, String objectName, String field) {
        return DefaultMessageCodesResolver.Format.toDelimitedString(objectName, field, formatErrorCode(errorCode));
    }

    private static String formatErrorCode(String errorCode) {
        StringBuilder buf = new StringBuilder();
        if (errorCode.startsWith("Not")) {
            errorCode = errorCode.substring(3);
        } else {
            buf.append("wrong-");
        }
        for (int i = 0; i < errorCode.length(); i++) {
            char c = errorCode.charAt(i);
            if (Character.isUpperCase(c) && i != 0) {
                buf.append('-');
            }
            buf.append(Character.toLowerCase(c));
        }
        return buf.toString();
    }

}
