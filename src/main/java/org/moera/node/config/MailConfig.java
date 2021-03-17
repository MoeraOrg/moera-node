package org.moera.node.config;

public class MailConfig {

    // Maximum rate of sending mails: <mailSendLimit> mails per <mailSendPeriod> minutes
    private int sendLimit = 10;
    private int sendPeriod = 10;
    private String replyToAddress;

    public int getSendLimit() {
        return sendLimit;
    }

    public void setSendLimit(int sendLimit) {
        this.sendLimit = sendLimit;
    }

    public int getSendPeriod() {
        return sendPeriod;
    }

    public void setSendPeriod(int sendPeriod) {
        this.sendPeriod = sendPeriod;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

}
