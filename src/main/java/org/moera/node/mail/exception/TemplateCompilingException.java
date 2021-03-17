package org.moera.node.mail.exception;

public class TemplateCompilingException extends MailServiceException {

    public TemplateCompilingException(String templateName) {
        super("Mail template compiling exception: " + templateName);
    }

    public TemplateCompilingException(String templateName, Throwable cause) {
        super("Mail template compiling exception: " + templateName, cause);
    }

}
