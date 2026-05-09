package backend.sync.utils;

import java.io.Serializable;

public class StartSyncRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  public String fileName;
  public String storagePath;
  public String cdcClassName;
  public byte[] EncryptedAESKey;

  public StartSyncRequest(String fileName, String storagePath, String cdcClassName, byte[] AESKey) {
    this.fileName = fileName;
    this.storagePath = storagePath;
    this.cdcClassName = cdcClassName;
    this.EncryptedAESKey = Encryptor.encryptAES(AESKey);
  }
}
