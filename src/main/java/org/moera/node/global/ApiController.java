package org.moera.node.global;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@CrossOrigin(value = "*", exposedHeaders = {HttpHeaders.RETRY_AFTER, "RateLimit-Policy"}, maxAge = 86400)
public @interface ApiController {
}
