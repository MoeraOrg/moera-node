package org.moera.node.config;

public class DirectServeConfig {

    private DirectServeSource source = DirectServeSource.NONE;
    private String secret = "";
    private String bucket;
    private String region;
    private String profile;

    public DirectServeSource getSource() {
        return source;
    }

    public void setSource(DirectServeSource source) {
        this.source = source;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

}
