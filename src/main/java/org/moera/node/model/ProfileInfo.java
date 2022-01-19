package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SourceFormat;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileInfo {

    private String fullName;
    private String gender;
    private String email;
    private String title;
    private String bioSrc;
    private SourceFormat bioSrcFormat;
    private String bioHtml;
    private AvatarInfo avatar;
    private FundraiserInfo[] fundraisers;
    private Map<String, String[]> operations;

    public ProfileInfo() {
    }

    public ProfileInfo(RequestContext requestContext, boolean includeSource) {
        Options options = requestContext.getOptions();
        fullName = requestContext.fullName();
        gender = options.getString("profile.gender");
        if (requestContext.isAdmin()) {
            email = options.getString("profile.email");
        }
        title = options.getString("profile.title");
        if (includeSource) {
            bioSrc = options.getString("profile.bio.src");
            bioSrcFormat = SourceFormat.forValue(options.getString("profile.bio.src.format"));
        }
        bioHtml = options.getString("profile.bio.html");
        if (requestContext.getAvatar() != null) {
            this.avatar = new AvatarInfo(requestContext.getAvatar());
        }
        fundraisers = FundraiserInfo.deserializeValue(options.getString("profile.fundraisers"));
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBioSrc() {
        return bioSrc;
    }

    public void setBioSrc(String bioSrc) {
        this.bioSrc = bioSrc;
    }

    public SourceFormat getBioSrcFormat() {
        return bioSrcFormat;
    }

    public void setBioSrcFormat(SourceFormat bioSrcFormat) {
        this.bioSrcFormat = bioSrcFormat;
    }

    public String getBioHtml() {
        return bioHtml;
    }

    public void setBioHtml(String bioHtml) {
        this.bioHtml = bioHtml;
    }

    public AvatarInfo getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarInfo avatar) {
        this.avatar = avatar;
    }

    public FundraiserInfo[] getFundraisers() {
        return fundraisers;
    }

    public void setFundraisers(FundraiserInfo[] fundraisers) {
        this.fundraisers = fundraisers;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
