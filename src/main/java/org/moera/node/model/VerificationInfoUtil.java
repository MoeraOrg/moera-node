package org.moera.node.model;

import java.util.Locale;

import org.moera.lib.node.types.VerificationInfo;
import org.springframework.context.MessageSource;

public class VerificationInfoUtil {

    public static VerificationInfo correct() {
        VerificationInfo info = new VerificationInfo();
        info.setCorrect(true);
        return info;
    }

    public static VerificationInfo incorrect(String errorCode, MessageSource messageSource) {
        VerificationInfo info = new VerificationInfo();
        info.setCorrect(false);
        info.setErrorCode(errorCode);
        info.setErrorMessage(messageSource.getMessage(errorCode, null, Locale.getDefault()));
        return info;
    }

}
