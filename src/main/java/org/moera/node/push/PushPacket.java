package org.moera.node.push;

import java.util.UUID;

public class PushPacket {

    private UUID id;
    private String text;

    public PushPacket(String text) {
        id = UUID.randomUUID();
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
