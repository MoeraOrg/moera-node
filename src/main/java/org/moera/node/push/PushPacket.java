package org.moera.node.push;

import org.moera.node.data.PushNotification;

public class PushPacket {

    private long moment;
    private String content;

    public PushPacket(long moment, String content) {
        this.moment = moment;
        this.content = content;
    }

    public PushPacket(PushNotification pushNotification) {
        this(pushNotification.getMoment(), pushNotification.getContent());
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
