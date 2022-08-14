package org.moera.node.model;

import javax.validation.constraints.NotBlank;

public class TokenAttributes {

    @NotBlank
    private String login;

    @NotBlank
    private String password;

    private Long authCategory;

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

    public Long getAuthCategory() {
        return authCategory;
    }

    public void setAuthCategory(Long authCategory) {
        this.authCategory = authCategory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
