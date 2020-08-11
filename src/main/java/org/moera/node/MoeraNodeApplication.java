package org.moera.node;

import java.security.Security;
import javax.inject.Inject;

import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.moera.node.auth.AuthenticationInterceptor;
import org.moera.node.domain.DomainInterceptor;
import org.moera.node.event.AfterCommitEventsInterceptor;
import org.moera.node.global.CacheControlInterceptor;
import org.moera.node.global.ClientIdInterceptor;
import org.moera.node.global.EntitledInterceptor;
import org.moera.node.global.NetworkLatencyInterceptor;
import org.moera.node.global.SyndFeedHttpMessageConverter;
import org.moera.node.global.VirtualPageInterceptor;
import org.moera.node.helper.HelperSource;
import org.moera.node.registrar.RegistrarInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class MoeraNodeApplication implements WebMvcConfigurer {

    private static Logger log = LoggerFactory.getLogger(MoeraNodeApplication.class);

    @Inject
    private RegistrarInterceptor registrarInterceptor;

    @Inject
    private DomainInterceptor domainInterceptor;

    @Inject
    private AuthenticationInterceptor authenticationInterceptor;

    @Inject
    private VirtualPageInterceptor virtualPageInterceptor;

    @Inject
    private NetworkLatencyInterceptor networkLatencyInterceptor;

    @Inject
    private CacheControlInterceptor cacheControlInterceptor;

    @Inject
    private ClientIdInterceptor clientIdInterceptor;

    @Inject
    private AfterCommitEventsInterceptor afterCommitEventsInterceptor;

    @Inject
    private EntitledInterceptor entitledInterceptor;

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
        registry.addInterceptor(registrarInterceptor).order(-2);
        registry.addInterceptor(domainInterceptor).order(-1);
        registry.addInterceptor(authenticationInterceptor);
        registry.addInterceptor(virtualPageInterceptor);
        registry.addInterceptor(networkLatencyInterceptor);
        registry.addInterceptor(cacheControlInterceptor);
        registry.addInterceptor(clientIdInterceptor);
        registry.addInterceptor(afterCommitEventsInterceptor);
        registry.addInterceptor(entitledInterceptor);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(handlebarsViewResolver());
    }

    @Bean
    public HttpMessageConverters customMessageConverters() {
        return new HttpMessageConverters(
                new SyndFeedHttpMessageConverter());
    }

    @Bean
    public TaskExecutor namingTaskExecutor() {
        return new TaskExecutorBuilder().corePoolSize(8).maxPoolSize(16).build();
    }

    @Bean
    public TaskExecutor remoteTaskExecutor() {
        return new TaskExecutorBuilder().corePoolSize(8).maxPoolSize(16).build();
    }

    @Bean
    public TaskExecutor notificationSenderTaskExecutor() {
        return new TaskExecutorBuilder().corePoolSize(8).maxPoolSize(64).build();
    }

    @Bean
    public TaskExecutor pickerTaskExecutor() {
        return new TaskExecutorBuilder().corePoolSize(8).maxPoolSize(16).build();
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(MoeraNodeApplication.class, args);
    }

}
