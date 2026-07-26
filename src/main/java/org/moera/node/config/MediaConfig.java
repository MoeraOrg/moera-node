package org.moera.node.config;

public class MediaConfig {

    private String path;
    private MediaServe serve = MediaServe.STREAM;
    private String accelPrefix = "/";
    private String cloudAccelPrefix = "/";
    private DirectServeConfig directServe = new DirectServeConfig();
    private String ocrService;
    private String ocrServiceKey;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public MediaServe getServe() {
        return serve;
    }

    public void setServe(MediaServe serve) {
        this.serve = serve;
    }

    public String getAccelPrefix() {
        return accelPrefix;
    }

    public void setAccelPrefix(String accelPrefix) {
        this.accelPrefix = accelPrefix;
    }

    public String getCloudAccelPrefix() {
        return cloudAccelPrefix;
    }

    public void setCloudAccelPrefix(String cloudAccelPrefix) {
        this.cloudAccelPrefix = cloudAccelPrefix;
    }

    public DirectServeConfig getDirectServe() {
        return directServe;
    }

    public void setDirectServe(DirectServeConfig directServe) {
        this.directServe = directServe;
    }

    public String getOcrService() {
        return ocrService;
    }

    public void setOcrService(String ocrService) {
        this.ocrService = ocrService;
    }

    public String getOcrServiceKey() {
        return ocrServiceKey;
    }

    public void setOcrServiceKey(String ocrServiceKey) {
        this.ocrServiceKey = ocrServiceKey;
    }

}
