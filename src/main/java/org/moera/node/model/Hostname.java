package org.moera.node.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HostnameValidator.class)
public @interface Hostname {

    String message() default "{org.moera.node.model.Hostname.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
