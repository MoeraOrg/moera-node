package org.moera.node.sse;

public class SsePacket {

    private long moment;
    private String content;

    public SsePacket(long moment, String content) {
        this.moment = moment;
        this.content = content;
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
