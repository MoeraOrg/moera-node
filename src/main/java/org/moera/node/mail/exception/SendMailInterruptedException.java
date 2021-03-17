package org.moera.node.mail.exception;

public class SendMailInterruptedException extends MailServiceException {

    public SendMailInterruptedException() {
        super("Send mail was interrupted");
    }

}
