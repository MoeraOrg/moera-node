package org.moera.node.operations;

import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.springframework.util.ObjectUtils;

public class LoginUtil {

    public static boolean isLoginDisabled(Options options) {
        return Boolean.TRUE.equals(options.getBool("credentials.login-disabled"));
    }

    public static boolean isCreated(Options options) {
        return isLoginDisabled(options)
            || !ObjectUtils.isEmpty(options.getString("credentials.login"))
            && !ObjectUtils.isEmpty(options.getString("credentials.password-hash"));
    }

    public static void checkLoginEnabled(Options options) {
        if (isLoginDisabled(options)) {
            throw new OperationFailure("credentials.login-disabled");
        }
    }

}
