package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "push_notifications")
public class PushNotification {

    @Id
    private UUID id;

    @ManyToOne
    @NotNull
    private PushClient pushClient;

    @NotNull
    private long moment;

    @NotNull
    private String content = "";

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PushClient getPushClient() {
        return pushClient;
    }

    public void setPushClient(PushClient pushClient) {
        this.pushClient = pushClient;
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
