package org.moera.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

@Configuration
@ConfigurationProperties("node")
public class Config {

    private String rootSecret;
    private String address;
    private boolean mockNetworkLatency;
    private PoolsConfig pools = new PoolsConfig();
    private MultiHost multi = MultiHost.NONE;
    private String domain;
    private RegistrarConfig registrar;
    private MailConfig mail = new MailConfig();
    private MediaConfig media = new MediaConfig();
    private OptionConfig[] options = new OptionConfig[0];
    private String fcmRelay;

    public String getRootSecret() {
        return rootSecret;
    }

    public void setRootSecret(String rootSecret) {
        this.rootSecret = rootSecret;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isMockNetworkLatency() {
        return mockNetworkLatency;
    }

    public void setMockNetworkLatency(boolean mockNetworkLatency) {
        this.mockNetworkLatency = mockNetworkLatency;
    }

    public PoolsConfig getPools() {
        return pools;
    }

    public void setPools(PoolsConfig pools) {
        this.pools = pools;
    }

    public MultiHost getMulti() {
        return multi;
    }

    public void setMulti(MultiHost multi) {
        this.multi = multi;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public RegistrarConfig getRegistrar() {
        return registrar;
    }

    public void setRegistrar(RegistrarConfig registrar) {
        this.registrar = registrar;
    }

    public boolean isRegistrarEnabled() {
        return getMulti() == MultiHost.PUBLIC
                && getRegistrar() != null
                && !ObjectUtils.isEmpty(getRegistrar().getHost())
                && !ObjectUtils.isEmpty(getRegistrar().getDomain());
    }

    public boolean isRegistrationPublic() {
        return getMulti() == MultiHost.PUBLIC
                && getRegistrar() != null
                && !ObjectUtils.isEmpty(getRegistrar().getDomain());
    }

    public MailConfig getMail() {
        return mail;
    }

    public void setMail(MailConfig mail) {
        this.mail = mail;
    }

    public MediaConfig getMedia() {
        return media;
    }

    public void setMedia(MediaConfig media) {
        this.media = media;
    }

    public OptionConfig[] getOptions() {
        return options;
    }

    public void setOptions(OptionConfig[] options) {
        this.options = options;
    }

    public String getFcmRelay() {
        return fcmRelay;
    }

    public void setFcmRelay(String fcmRelay) {
        this.fcmRelay = fcmRelay;
    }

}
