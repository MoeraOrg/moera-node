package org.moera.node.model;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class HostnameValidator implements ConstraintValidator<Hostname, String> {

    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile("^[a-z][a-z0-9-]*[a-z0-9](\\.[a-z][a-z0-9-]*[a-z0-9])*$", Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(Hostname constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return HOSTNAME_PATTERN.matcher(s).matches();
    }

}
