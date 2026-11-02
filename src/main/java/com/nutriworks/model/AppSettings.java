package com.nutriworks.model;

/**
 * Global persistent configuration settings for the desktop application.
 */
public class AppSettings {

    private String theme = "LIGHT"; // "LIGHT" or "DARK"
    private int serverPort = 8080;
    private String lanIpOverride = "";
    private boolean useStubClassifier = false;
    private boolean autoNavigateOnUpload = true;
    private int tokenTimeoutMinutes = 15;
    private boolean useHttps = false;

    public AppSettings() {
    }

    public String getTheme() {
        return theme != null ? theme : "LIGHT";
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getServerPort() {
        return serverPort > 0 ? serverPort : 8080;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getLanIpOverride() {
        return lanIpOverride != null ? lanIpOverride : "";
    }

    public void setLanIpOverride(String lanIpOverride) {
        this.lanIpOverride = lanIpOverride;
    }

    public boolean isUseStubClassifier() {
        return useStubClassifier;
    }

    public void setUseStubClassifier(boolean useStubClassifier) {
        this.useStubClassifier = useStubClassifier;
    }

    public boolean isAutoNavigateOnUpload() {
        return autoNavigateOnUpload;
    }

    public void setAutoNavigateOnUpload(boolean autoNavigateOnUpload) {
        this.autoNavigateOnUpload = autoNavigateOnUpload;
    }

    public int getTokenTimeoutMinutes() {
        return tokenTimeoutMinutes > 0 ? tokenTimeoutMinutes : 15;
    }

    public void setTokenTimeoutMinutes(int tokenTimeoutMinutes) {
        this.tokenTimeoutMinutes = tokenTimeoutMinutes;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }
}
