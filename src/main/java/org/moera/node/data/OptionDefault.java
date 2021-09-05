package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "option_defaults")
public class OptionDefault {

    @Id
    private UUID id;

    @NotNull
    @Size(max = 128)
    private String name;

    @Size(max = 4096)
    private String value;

    private Boolean privileged;

    public OptionDefault() {
    }

    public OptionDefault(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getPrivileged() {
        return privileged;
    }

    public void setPrivileged(Boolean privileged) {
        this.privileged = privileged;
    }

}
