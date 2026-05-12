package backend.model;

public class ClientConfig {

  private String serverBaseUrl;
  private String syncHost;
  private Integer syncPort;
  private boolean configured;

  public ClientConfig() {}

  public ClientConfig(String serverBaseUrl, String syncHost, Integer syncPort, boolean configured) {
    this.serverBaseUrl = serverBaseUrl;
    this.syncHost = syncHost;
    this.syncPort = syncPort;
    this.configured = configured;
  }

  public String getServerBaseUrl() {
    return serverBaseUrl;
  }

  public void setServerBaseUrl(String serverBaseUrl) {
    this.serverBaseUrl = serverBaseUrl;
  }

  public String getSyncHost() {
    return syncHost;
  }

  public void setSyncHost(String syncHost) {
    this.syncHost = syncHost;
  }

  public Integer getSyncPort() {
    return syncPort;
  }

  public void setSyncPort(Integer syncPort) {
    this.syncPort = syncPort;
  }

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
  }
}
