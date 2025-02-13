package org.moera.node.model;

import java.util.Map;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.FundraiserInfo;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.model.constraint.Uuid;
import org.moera.node.option.Options;
import org.moera.node.text.TextConverter;
import org.springframework.util.ObjectUtils;

public class ProfileAttributes {

    @Size(max = 96)
    private String fullName;

    @Size(max = 31)
    private String gender;

    @Email
    @Size(max = 63)
    private String email;

    @Size(max = 120)
    private String title;

    @Size(max = 4096)
    private String bioSrc;

    private SourceFormat bioSrcFormat = SourceFormat.MARKDOWN;

    @Uuid
    private String avatarId; // not UUID type, because empty string is allowed

    private FundraiserInfo[] fundraisers;

    private Map<String, Principal> operations;

    public ProfileAttributes() {
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

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public FundraiserInfo[] getFundraisers() {
        return fundraisers;
    }

    public void setFundraisers(FundraiserInfo[] fundraisers) {
        this.fundraisers = fundraisers;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    @Transactional
    public void toOptions(Options options, TextConverter textConverter) {
        String bioHtml;
        if (!ObjectUtils.isEmpty(getBioSrc()) && getBioSrcFormat() != SourceFormat.APPLICATION) {
            bioHtml = textConverter.toHtml(getBioSrcFormat(), getBioSrc());
        } else {
            bioHtml = getBioSrc();
        }
        options.runInTransaction(opt -> {
            toOption("profile.full-name", getFullName(), opt);
            toOption("profile.gender", getGender(), opt);
            toOption("profile.email", getEmail(), opt);
            toOption("profile.email.view", getPrincipal("viewEmail"), opt);
            toOption("profile.title", getTitle(), opt);
            toOption("profile.bio.src", getBioSrc(), opt);
            toOption("profile.bio.src.format", getBioSrcFormat().getValue(), opt);
            toOption("profile.bio.html", bioHtml, opt);
            toOption("profile.avatar.id", getAvatarId(), opt);
            toOption("profile.fundraisers", FundraiserInfo.serializeValue(getFundraisers()), opt);
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

    private void toOption(String name, Principal value, Options options) {
        if (value != null) {
            if (!value.isUnset()) {
                options.set(name, value);
            } else {
                options.reset(name);
            }
        }
    }

}
