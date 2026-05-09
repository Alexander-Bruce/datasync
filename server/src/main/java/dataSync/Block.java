package dataSync;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block implements Serializable {
  private static final long serialVersionUID = 1L;

  private final BigInteger index;
  private final int chunkSize;
  private final String hashCode;
  private long offset;

  public Block(int chunkSize, BigInteger index, long offset, byte[] chunk) {
    this.chunkSize = chunkSize;
    this.index = index;
    // 保存原始数据，以便客户端发现服务端缺少该块时发送数据
    this.offset = offset;
    this.hashCode = bytesToHex(sha256(chunk));
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public String getHashCode() {
    return hashCode;
  }

  public long getOffset() {
    return offset;
  }

  public static byte[] sha256(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  public static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      hexString.append(String.format("%02x", b & 0xFF));
    }
    return hexString.toString();
  }
}
