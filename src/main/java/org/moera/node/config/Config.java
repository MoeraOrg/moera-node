package org.moera.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties("node")
public class Config {

    private String rootSecret;
    private String address;
    private boolean mockNetworkLatency;
    private MultiHost multi = MultiHost.NONE;
    private RegistrarConfig registrar;

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

    public MultiHost getMulti() {
        return multi;
    }

    public void setMulti(MultiHost multi) {
        this.multi = multi;
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
                && !StringUtils.isEmpty(getRegistrar().getHost())
                && !StringUtils.isEmpty(getRegistrar().getDomain());
    }

    public boolean isRegistrationPublic() {
        return getMulti() == MultiHost.PUBLIC
                && getRegistrar() != null
                && !StringUtils.isEmpty(getRegistrar().getDomain());
    }

}
