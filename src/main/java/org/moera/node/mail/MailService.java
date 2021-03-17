package org.moera.node.mail;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import org.moera.node.config.Config;
import org.moera.node.domain.Domains;
import org.moera.node.mail.exception.MailServiceException;
import org.moera.node.mail.exception.SendMailInterruptedException;
import org.moera.node.mail.exception.TemplateCompilingException;
import org.moera.node.xml.XmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private static final String MAILROBOT_PREFIX = "mailrobot@";
    private static final String TEMPLATES_DIRECTORY = "mail/";

    private static Logger log = LoggerFactory.getLogger(MailService.class);

    @Inject
    private Config config;

    @Inject
    private Domains domains;

    @Inject
    private JavaMailSender mailSender;

    @Inject
    @Lazy
    private HandlebarsViewResolver handlebarsViewResolver;

    private BlockingQueue<MimeMessagePreparator> mailQueue = new LinkedBlockingQueue<>();

    private Map<String, Template> compiledTemplates = new HashMap<>();

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
        String domainName = domains.getDomainName(nodeId);

        MDC.put("domain", domainName);

        mail.setDomainName(domainName);
        mail.setEmail(domains.getDomainOptions(nodeId).getString("profile.email"));
        try {
            send(mail);
        } catch (MailServiceException e) {
            log.error("Error sending email to {}: {}", mail.getEmail(), e.getMessage());
        }
    }

    public void send(Mail mail) throws MailServiceException {
        if (StringUtils.isEmpty(mail.getDomainName()) || mail.getDomainName().equals(Domains.DEFAULT_DOMAIN)
                || StringUtils.isEmpty(mail.getEmail())) {
            return;
        }

        try {
            mailQueue.put(mimeMessage -> {
                String document = getDocument(mail.getTemplateName(), mail.getModel());
                MailXmlToText handler = new MailXmlToText();
                XmlConverter.convert(document, handler);

                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, "UTF-8");
                message.setTo(mail.getEmail());
                message.setFrom(MAILROBOT_PREFIX + mail.getDomainName());
                String replyTo = config.getMail().getReplyToAddress();
                if (!StringUtils.isEmpty(replyTo)) {
                    message.setReplyTo(replyTo);
                }
                message.setSubject(handler.getResult().getSubject().toString());
                message.setText(handler.getResult().getBody().toString());
            });
        } catch (InterruptedException e) {
            throw new SendMailInterruptedException();
        }
    }

    private String getDocument(String templateName, Map<String, Object> model) throws MailServiceException {
        Template template = getTemplate(templateName);
        try {
            return template.apply(model);
        } catch (IOException e) {
            log.error("I/O error when compiling template: " + templateName, e);
            throw new TemplateCompilingException(templateName, e);
        }
    }

    private Template getTemplate(String templateName) throws MailServiceException {
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
        Deque<Instant> sent = new ArrayDeque<>();

        while (true) {
            try {
                while (sent.size() >= config.getMail().getSendLimit()) {
                    while (sent.peekFirst() != null && sent.peekFirst()
                            .isBefore(Instant.now().minus(config.getMail().getSendPeriod(), ChronoUnit.MINUTES))) {
                        sent.pollFirst();
                    }
                    if (sent.size() < config.getMail().getSendLimit()) {
                        break;
                    }
                    Thread.sleep(1000);
                }
                log.info("Delivering an e-mail");
                mailSender.send(mailQueue.take());
                sent.offerLast(Instant.now());
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

}
