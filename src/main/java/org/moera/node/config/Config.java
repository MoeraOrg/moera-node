package org.moera.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("node")
public class Config {

    private String rootSecret;
    private String address;
    private boolean mockNetworkLatency;
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

    public RegistrarConfig getRegistrar() {
        return registrar;
    }

    public void setRegistrar(RegistrarConfig registrar) {
        this.registrar = registrar;
    }

}
