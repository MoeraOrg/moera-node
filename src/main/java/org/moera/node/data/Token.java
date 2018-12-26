package org.moera.node.data;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @Size(max = 45)
    private String token;

    @Size(max = 127)
    private String name;

    private Integer generation;

    @NotNull
    private boolean admin;

    @NotNull
    private Timestamp created = Util.now();

    @NotNull
    private Timestamp deadline;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

}
