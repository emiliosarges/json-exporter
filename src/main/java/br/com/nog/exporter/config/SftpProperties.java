package br.com.nog.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sftp")
public class SftpProperties {

    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String remoteDirectory = "/upload";
    private boolean allowUnknownKeys;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    public boolean isAllowUnknownKeys() {
        return allowUnknownKeys;
    }

    public void setAllowUnknownKeys(boolean allowUnknownKeys) {
        this.allowUnknownKeys = allowUnknownKeys;
    }
}
