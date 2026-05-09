package backend.exception.model;

public class EncryptException extends RuntimeException {
  private Integer code;

  public EncryptException(String msg, Integer code) {
    super(msg);
    this.code = code;
  }
}
