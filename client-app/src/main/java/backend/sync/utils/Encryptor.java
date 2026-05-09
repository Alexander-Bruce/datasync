package backend.sync.utils;

import backend.exception.model.EncryptException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final int IV_LEN = 12;
  private static final int TAG_LEN = 128;
  private static String publicKey;
  private static String privateKey;

  static {
    try {
      // 1st: load from classpath (present when built with mvn and keys committed)
      publicKey = readPem("conf/rsa-public.pem");
      privateKey = readPem("conf/rsa-private.pem");
    } catch (Exception ignored) {
      // 2nd: load from / auto-generate into shared user-home directory.
      // Both client-app and server-app look in the same location, so whoever
      // starts first generates the pair and the other app reuses it.
      try {
        Path dir = Paths.get(System.getProperty("user.home"), ".datasync", "conf");
        Files.createDirectories(dir);
        Path pub = dir.resolve("rsa-public.pem");
        Path pri = dir.resolve("rsa-private.pem");

        if (!Files.exists(pub) || !Files.exists(pri)) {
          KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
          kpg.initialize(2048, new SecureRandom());
          KeyPair kp = kpg.generateKeyPair();
          writeKeyFile(pub, "PUBLIC KEY", kp.getPublic().getEncoded());
          writeKeyFile(pri, "PRIVATE KEY", kp.getPrivate().getEncoded());
          System.err.printf("[Encryptor] Auto-generated RSA 2048 key pair → %s%n", dir);
        }

        publicKey = readPemFromPath(pub);
        privateKey = readPemFromPath(pri);
      } catch (Exception fallback) {
        throw new ExceptionInInitializerError("RSA Key load failed: " + fallback.getMessage());
      }
    }
  }

  public static byte[] encrypt(byte[] plain, byte[] key) {
    try {
      byte[] iv = new byte[IV_LEN];
      SecureRandom random = new SecureRandom();
      random.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(AES_GCM);
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LEN, iv);

      cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
      byte[] cipherText = cipher.doFinal(plain);

      // iv + ciphertext
      byte[] out = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
      return out;
    } catch (Exception e) {
      throw new EncryptException("Encryption Error", 501);
    }
  }

  public static byte[] decrypt(byte[] encrypted, byte[] key) {
    try {
      byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_LEN);
      byte[] cipherText = Arrays.copyOfRange(encrypted, IV_LEN, encrypted.length);

      Cipher cipher = Cipher.getInstance(AES_GCM);
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LEN, iv);

      cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
      return cipher.doFinal(cipherText);
    } catch (Exception e) {
      throw new EncryptException("Decryption Error", 502);
    }
  }

  public static byte[] genKey() {
    try {
      KeyGenerator kg = KeyGenerator.getInstance("AES");
      kg.init(256);
      return kg.generateKey().getEncoded();
    } catch (Exception e) {
      throw new EncryptException("Key Generation Error", 503);
    }
  }

  public static byte[] encryptAES(byte[] aesKey) {
    try {
      PublicKey pubKey = Encryptor.loadPublicKey();

      Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey);

      return rsaCipher.doFinal(aesKey);
    } catch (Exception e) {
      throw new EncryptException("Key Generation Error", 503);
    }
  }

  public static byte[] decryptAESBytes(byte[] encryptedAesKey) {
    try {
      PrivateKey priKey = Encryptor.loadPrivateKey();

      Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      rsaCipher.init(Cipher.DECRYPT_MODE, priKey);

      return rsaCipher.doFinal(encryptedAesKey);
    } catch (Exception e) {
      throw new EncryptException("Key Generation Error", 503);
    }
  }

  public static PublicKey loadPublicKey() {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(publicKey);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      return factory.generatePublic(spec);
    } catch (Exception e) {
      throw new EncryptException("Key Generation Error", 503);
    }
  }

  public static PrivateKey loadPrivateKey() {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(privateKey);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      return factory.generatePrivate(spec);
    } catch (Exception e) {
      throw new EncryptException("Key Generation Error", 503);
    }
  }

  // ── private helpers ───────────────────────────────────────────────────────

  private static String readPem(String resourcePath) throws Exception {
    try (InputStream is = Encryptor.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) throw new FileNotFoundException(resourcePath);
      return new String(is.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("-----BEGIN [A-Z ]+-----", "")
          .replaceAll("-----END [A-Z ]+-----", "")
          .replaceAll("\\s", "");
    }
  }

  private static String readPemFromPath(Path path) throws Exception {
    return Files.readString(path, StandardCharsets.UTF_8)
        .replaceAll("-----BEGIN [A-Z ]+-----", "")
        .replaceAll("-----END [A-Z ]+-----", "")
        .replaceAll("\\s", "");
  }

  private static void writeKeyFile(Path path, String type, byte[] encoded) throws Exception {
    String b64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);
    String pem = "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    try {
      // CREATE_NEW is atomic — only one process wins if both try simultaneously
      Files.writeString(
          path,
          pem,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
    } catch (java.nio.file.FileAlreadyExistsException ignored) {
      // Another process wrote first — use their file
    }
  }
}
