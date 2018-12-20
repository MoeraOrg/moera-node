package org.moera.node;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("node")
public class Config {

    private static Config instance;

    private String namingServer;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static Config getInstance() {
        return instance;
    }

    public String getNamingServer() {
        return namingServer;
    }

    public void setNamingServer(String namingServer) {
        this.namingServer = namingServer;
    }

}
