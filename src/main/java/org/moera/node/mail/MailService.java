package org.moera.node.mail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import io.github.bucket4j.Bucket;
import org.moera.node.config.Config;
import org.moera.node.data.VerifiedEmailRepository;
import org.moera.node.domain.Domains;
import org.moera.node.mail.exception.MailServiceException;
import org.moera.node.mail.exception.SendMailInterruptedException;
import org.moera.node.mail.exception.TemplateCompilingException;
import org.moera.node.xml.XmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class MailService {

    private static final String MAILROBOT_PREFIX = "mailrobot@";
    private static final String TEMPLATES_DIRECTORY = "mail/";
    private static final String SUBJECT_PREFIX = "[Moera] ";

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Inject
    private Config config;

    @Inject
    private Domains domains;

    @Inject
    private VerifiedEmailRepository verifiedEmailRepository;

    @Inject
    private JavaMailSender mailSender;

    @Inject
    @Lazy
    private HandlebarsViewResolver handlebarsViewResolver;

    @Inject
    private ApplicationContext applicationContext;

    private final BlockingQueue<MimeMessagePreparator> mailQueue = new LinkedBlockingQueue<>();

    private final Map<String, Template> compiledTemplates = new HashMap<>();

    private Handlebars getHandlebars() {
        return handlebarsViewResolver.getHandlebars();
    }

    @PostConstruct
    public void init() {
        Thread thread = new Thread(this::runMailQueue);
        thread.setDaemon(true);
        thread.start();
    }

    public void send(UUID nodeId, Mail mail) {
        send(nodeId, mail, false);
    }

    public void sendToRoot(UUID nodeId, Mail mail) {
        send(nodeId, mail, true);
    }

    public void send(UUID nodeId, Mail mail, boolean toRoot) {
        MDC.put("domain", domains.getDomainEffectiveName(nodeId));

        mail.setDomainName(domains.getDomainDnsName(nodeId));
        if (toRoot) {
            mail.setEmail(config.getMail().getRootAddress());
        } else {
            String email = domains.getDomainOptions(nodeId).getString("profile.email");
            if (mail.verifiedAddressOnly()) {
                boolean verified = verifiedEmailRepository.countByNodeIdAndEmail(nodeId, email) > 0;
                if (!verified) {
                    log.warn("E-mail address {} is not verified, have no right to send", mail.getEmail());
                    return;
                }
            }
            mail.setEmail(email);
        }
        try {
            send(mail);
        } catch (MailServiceException e) {
            log.error("Error sending email to {}: {}", mail.getEmail(), e.getMessage());
        }
    }

    private void send(Mail mail) throws MailServiceException {
        if (
            ObjectUtils.isEmpty(mail.getDomainName())
            || mail.getDomainName().equals(Domains.DEFAULT_DOMAIN)
            || ObjectUtils.isEmpty(mail.getEmail())
        ) {
            return;
        }

        try {
            mailQueue.put(mimeMessage -> {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                message.setTo(mail.getEmail());
                message.setFrom(MAILROBOT_PREFIX + mail.getDomainName());
                String replyTo = config.getMail().getReplyToAddress();
                if (!ObjectUtils.isEmpty(replyTo)) {
                    message.setReplyTo(replyTo);
                }

                String document = getDocument(mail.getTemplateName(), false, mail.getModel());
                MailXmlToText handler = new MailXmlToText();
                XmlConverter.convert(document, handler);
                var plainText = handler.getResult();

                var html = getDocument(mail.getTemplateName(), true, mail.getModel());

                message.setSubject(SUBJECT_PREFIX + plainText.getSubject().toString());
                message.setText(plainText.getBody().toString(), html);
                message.addInline("logo.png", applicationContext.getResource("classpath:templates/mail/part/logo.png"));
            });
        } catch (InterruptedException e) {
            throw new SendMailInterruptedException();
        }
    }

    private String getDocument(
        String templateName, boolean html, Map<String, Object> model
    ) throws MailServiceException {
        Template template = getTemplate(templateName, html);
        try {
            return template.apply(model);
        } catch (IOException e) {
            log.error("I/O error when compiling template: " + templateName, e);
            throw new TemplateCompilingException(templateName, e);
        }
    }

    private Template getTemplate(String templateName, boolean html) throws MailServiceException {
        if (html) {
            templateName += ".html";
        }
        Template template = compiledTemplates.get(templateName);
        if (template == null) {
            try {
                template = getHandlebars().compile(TEMPLATES_DIRECTORY + templateName);
            } catch (IOException e) {
                log.error("I/O error when compiling template: " + templateName, e);
                throw new TemplateCompilingException(templateName, e);
            }
            compiledTemplates.put(templateName, template);
        }
        return template;
    }

    private void runMailQueue() {
        var bucket = Bucket.builder()
            .addLimit(limit ->
                limit
                    .capacity(config.getMail().getSendLimit())
                    .refillGreedy(config.getMail().getSendLimit(), Duration.ofMinutes(config.getMail().getSendPeriod()))
            )
            .build();

        while (true) {
            try {
                while (!bucket.tryConsume(1)) {
                    long retryAfter = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1000000 + 1;
                    Thread.sleep(retryAfter);
                }
                log.info("Delivering an e-mail");
                mailSender.send(mailQueue.take());
            } catch (Exception e) {
                log.error("Error delivering e-mail:", e);
            }
        }
    }

}
