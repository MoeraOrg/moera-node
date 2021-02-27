package org.moera.node.model;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

import org.moera.node.data.SourceFormat;
import org.moera.node.option.Options;
import org.moera.node.text.TextConverter;
import org.springframework.util.StringUtils;

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

    @Transactional
    public void toOptions(Options options, TextConverter textConverter) {
        String bioHtml;
        if (!StringUtils.isEmpty(getBioSrc()) && getBioSrcFormat() != SourceFormat.APPLICATION) {
            bioHtml = textConverter.toHtml(getBioSrcFormat(), getBioSrc());
        } else {
            bioHtml = getBioSrc();
        }
        options.runInTransaction(opt -> {
            toOption("profile.full-name", getFullName(), opt);
            toOption("profile.gender", getGender(), opt);
            toOption("profile.email", getEmail(), opt);
            toOption("profile.title", getTitle(), opt);
            toOption("profile.bio.src", getBioSrc(), opt);
            toOption("profile.bio.src.format", getBioSrcFormat().getValue(), opt);
            toOption("profile.bio.html", bioHtml, opt);
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
