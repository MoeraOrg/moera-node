package org.moera.node.config;

public class MediaConfig {

    private String path;
    private String serve = "stream"; // stream, sendfile, accel
    private String accelPrefix = "/";
    private boolean directServe;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getServe() {
        return serve;
    }

    public void setServe(String serve) {
        this.serve = serve;
    }

    public String getAccelPrefix() {
        return accelPrefix;
    }

    public void setAccelPrefix(String accelPrefix) {
        this.accelPrefix = accelPrefix;
    }

    public boolean isDirectServe() {
        return directServe;
    }

    public void setDirectServe(boolean directServe) {
        this.directServe = directServe;
    }

}
