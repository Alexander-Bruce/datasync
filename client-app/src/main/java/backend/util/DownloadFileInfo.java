package backend.util;

import java.io.Serializable;

/** 下行同步时，从服务端拉取的单文件信息。 relativePath 是相对于同步范围根目录的路径； contentBase64 是文件内容的 Base64 编码。 */
public class DownloadFileInfo implements Serializable {

  private String relativePath;

  private String contentBase64;

  public DownloadFileInfo() {}

  public DownloadFileInfo(String relativePath, String contentBase64) {
    this.relativePath = relativePath;
    this.contentBase64 = contentBase64;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public String getContentBase64() {
    return contentBase64;
  }

  public void setContentBase64(String contentBase64) {
    this.contentBase64 = contentBase64;
  }
}
