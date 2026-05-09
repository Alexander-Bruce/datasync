package backend.exception.model;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

  private Integer code;

  public BaseException() {}

  public BaseException(String msg, Integer code) {
    super(msg);
    this.code = code;
  }
}
