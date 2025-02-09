package org.moera.node.model.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UuidValidator.class)
public @interface Uuid {

    String message() default "{org.moera.node.model.constraint.Uuid.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
