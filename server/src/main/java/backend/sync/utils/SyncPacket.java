package backend.sync.utils;

import java.io.Serializable;

public class SyncPacket implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Type {
    REFERENCE,
    DATA,
    EOF
  }

  public Type type;
  public String hash;
  private byte[] data;

  public SyncPacket(Type type, String hash, byte[] data, byte[] key) {
    this.type = type;
    this.hash = hash;
    if (key != null) this.data = Encryptor.encrypt(data, key);
    else this.data = null;
  }

  public byte[] getData(byte[] key) {
    return Encryptor.decrypt(this.data, key);
  }
}
