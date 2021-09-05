package org.moera.node.model.constraint;

import java.util.UUID;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.ObjectUtils;

public class UuidValidator implements ConstraintValidator<Uuid, String> {

    @Override
    public void initialize(Uuid constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (ObjectUtils.isEmpty(s)) {
            return true;
        }
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
