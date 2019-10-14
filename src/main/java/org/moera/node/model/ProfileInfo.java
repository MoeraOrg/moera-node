package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileInfo {

    private String fullName;
    private String gender;
    private String email;
    private Map<String, String[]> operations;

    public ProfileInfo() {
    }

    public ProfileInfo(RequestContext requestContext) {
        Options options = requestContext.getOptions();
        fullName = options.getString("profile.full-name");
        gender = options.getString("profile.gender");
        if (requestContext.isAdmin()) {
            email = options.getString("profile.email");
        }
        operations = Collections.singletonMap("edit", new String[]{"admin"});
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

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
