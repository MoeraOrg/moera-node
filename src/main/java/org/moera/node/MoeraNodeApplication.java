package org.moera.node;

import java.security.Security;
import javax.inject.Inject;

import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.moera.node.global.PermissionsInterceptor;
import org.moera.node.global.CacheControlInterceptor;
import org.moera.node.global.NetworkLatencyInterceptor;
import org.moera.node.global.VirtualPageInterceptor;
import org.moera.node.helper.HelperSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class MoeraNodeApplication implements WebMvcConfigurer {

    private static Logger log = LoggerFactory.getLogger(MoeraNodeApplication.class);

    @Inject
    private PermissionsInterceptor permissionsInterceptor;

    @Inject
    private VirtualPageInterceptor virtualPageInterceptor;

    @Inject
    private NetworkLatencyInterceptor networkLatencyInterceptor;

    @Inject
    private CacheControlInterceptor cacheControlInterceptor;

    @Inject
    private ApplicationContext applicationContext;

    @Bean
    public HandlebarsViewResolver handlebarsViewResolver() {
        HandlebarsViewResolver resolver = new HandlebarsViewResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".hbs.html");
        for (Object helperSource : applicationContext.getBeansWithAnnotation(HelperSource.class).values()) {
            log.info("Registering Handlebars helper class {}", helperSource.getClass().getName());
            resolver.registerHelpers(helperSource);
        }
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionsInterceptor);
        registry.addInterceptor(virtualPageInterceptor);
        registry.addInterceptor(networkLatencyInterceptor);
        registry.addInterceptor(cacheControlInterceptor);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(handlebarsViewResolver());
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(MoeraNodeApplication.class, args);
    }

}
