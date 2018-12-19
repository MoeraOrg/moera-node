package org.moera.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class MoeraNodeApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(MoeraNodeApplication.class, args);
    }

}
