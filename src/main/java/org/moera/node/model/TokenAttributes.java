package org.moera.node.model;

import java.util.List;
import javax.validation.constraints.NotBlank;

public class TokenAttributes {

    @NotBlank
    private String login;

    @NotBlank
    private String password;

    private List<String> permissions;

    private String name;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
