package org.moera.node;

import java.security.Security;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.moera.node.auth.AuthenticationInterceptor;
import org.moera.node.auth.SearchEngineInterceptor;
import org.moera.node.auth.UserAgentInterceptor;
import org.moera.node.config.Config;
import org.moera.node.domain.DomainInterceptor;
import org.moera.node.global.CacheControlInterceptor;
import org.moera.node.global.ClientIdInterceptor;
import org.moera.node.global.EntitledInterceptor;
import org.moera.node.global.NetworkLatencyInterceptor;
import org.moera.node.global.RateLimitInterceptor;
import org.moera.node.global.RequestRateInterceptor;
import org.moera.node.global.SlowRequestsInnerInterceptor;
import org.moera.node.global.SlowRequestsInterceptor;
import org.moera.node.global.SyndFeedHttpMessageConverter;
import org.moera.node.global.VirtualPageInterceptor;
import org.moera.node.liberin.AfterCommitLiberinsInterceptor;
import org.moera.node.ui.helper.HelperSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class MoeraNodeApplication implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MoeraNodeApplication.class);

    @Inject
    private Config config;

    @Inject
    private DomainInterceptor domainInterceptor;

    @Inject
    private UserAgentInterceptor userAgentInterceptor;

    @Inject
    private AuthenticationInterceptor authenticationInterceptor;

    @Inject
    private SearchEngineInterceptor searchEngineInterceptor;

    @Inject
    private VirtualPageInterceptor virtualPageInterceptor;

    @Inject
    private NetworkLatencyInterceptor networkLatencyInterceptor;

    @Inject
    private CacheControlInterceptor cacheControlInterceptor;

    @Inject
    private ClientIdInterceptor clientIdInterceptor;

    @Inject
    private AfterCommitLiberinsInterceptor afterCommitLiberinsInterceptor;

    @Inject
    private EntitledInterceptor entitledInterceptor;

    @Inject
    private SlowRequestsInterceptor slowRequestsInterceptor;

    @Inject
    private SlowRequestsInnerInterceptor slowRequestsInnerInterceptor;

    @Inject
    private RequestRateInterceptor requestRateInterceptor;

    @Inject
    private RateLimitInterceptor rateLimitInterceptor;

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
        registry.addInterceptor(requestRateInterceptor).order(-5);
        if (config.getDebug().isLogSlowRequests()) {
            registry.addInterceptor(slowRequestsInterceptor).order(-4);
        }
        registry.addInterceptor(domainInterceptor).order(-3);
        registry.addInterceptor(userAgentInterceptor).order(-3);
        registry.addInterceptor(searchEngineInterceptor).order(-2);
        registry.addInterceptor(authenticationInterceptor).order(-2);
        registry.addInterceptor(rateLimitInterceptor).order(-1);
        registry.addInterceptor(virtualPageInterceptor);
        registry.addInterceptor(networkLatencyInterceptor);
        registry.addInterceptor(cacheControlInterceptor);
        registry.addInterceptor(clientIdInterceptor);
        registry.addInterceptor(afterCommitLiberinsInterceptor);
        registry.addInterceptor(entitledInterceptor);
        if (config.getDebug().isLogSlowRequests()) {
            registry.addInterceptor(slowRequestsInnerInterceptor).order(1);
        }
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
        return buildTaskExecutor(config.getPools().getNaming());
    }

    @Bean
    public TaskExecutor remoteTaskExecutor() {
        return buildTaskExecutor(config.getPools().getRemoteTask());
    }

    @Bean
    public ThreadPoolTaskExecutor notificationSenderTaskExecutor() {
        return buildTaskExecutor(config.getPools().getNotificationSender());
    }

    @Bean
    public TaskExecutor pickerTaskExecutor() {
        return buildTaskExecutor(config.getPools().getPicker());
    }

    @Bean
    public TaskExecutor pushTaskExecutor() {
        return buildTaskExecutor(config.getPools().getPush());
    }

    private ThreadPoolTaskExecutor buildTaskExecutor(int size) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(size);
        taskExecutor.setMaxPoolSize(size);
        return taskExecutor;
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(MoeraNodeApplication.class, args);
    }

}
