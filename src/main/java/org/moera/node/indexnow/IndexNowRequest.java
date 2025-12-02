package org.moera.node.indexnow;

import java.util.List;

public class IndexNowRequest {

    private String host;
    private String key;
    private String keyLocation;
    private List<String> urlList;

    public IndexNowRequest(String host, String key, List<String> urlList) {
        this.host = host;
        this.key = key;
        this.keyLocation = String.format("https://%s/index-now-key.txt", host);
        this.urlList = urlList;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyLocation() {
        return keyLocation;
    }

    public void setKeyLocation(String keyLocation) {
        this.keyLocation = keyLocation;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }

}
