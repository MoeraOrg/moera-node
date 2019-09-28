package org.moera.node.model;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

import org.moera.node.option.Options;

public class Profile {

    @Size(max = 255)
    private String fullName;

    @Size(max = 31)
    private String gender;

    @Email
    @Size(max = 63)
    private String email;

    public Profile() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Transactional
    public void toOptions(Options options) {
        options.runInTransaction(opt -> {
            toOption("profile.full-name", getFullName(), opt);
            toOption("profile.gender", getGender(), opt);
            toOption("profile.email", getEmail(), opt);
        });
    }

    private void toOption(String name, String value, Options options) {
        if (value != null) {
            if (!value.isEmpty()) {
                options.set(name, value);
            } else {
                options.reset(name);
            }
        }
    }

}
