package backend.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResultEntity<T> {

  private int code;
  private String message;
  private long time;
  private T data;

  public ResultEntity(final int code, final String message, final long currentTimeMillis) {
    this.code = code;
    this.message = message;
    this.time = currentTimeMillis;
  }

  public static Integer getCurrentTime() {
    return (int) (System.currentTimeMillis() / 1000);
  }

  public static <T> ResponseEntity<ResultEntity<T>> success(Integer code, String message, T data) {
    ResultEntity<T> resultEntity = new ResultEntity<>(code, message, getCurrentTime(), data);
    return ResponseEntity.status(HttpStatus.OK).body(resultEntity);
  }

  public static <T> ResponseEntity<ResultEntity<T>> success(Integer code, String message) {
    ResultEntity<T> resultEntity = new ResultEntity<>(code, message, getCurrentTime());
    return ResponseEntity.status(HttpStatus.OK).body(resultEntity);
  }

  public static <T> ResponseEntity<ResultEntity<T>> error(Integer code, String message, T data) {
    ResultEntity<T> resultEntity = new ResultEntity<>(code, message, getCurrentTime(), data);
    return ResponseEntity.status(HttpStatus.OK).body(resultEntity);
  }

  public static <T> ResponseEntity<ResultEntity<T>> error(Integer code, String message) {
    ResultEntity<T> resultEntity = new ResultEntity<>(code, message, getCurrentTime());
    return ResponseEntity.status(HttpStatus.OK).body(resultEntity);
  }

  public static <T> ResponseEntity<ResultEntity<T>> error(String message) {
    ResultEntity<T> resultEntity =
        new ResultEntity<>(HttpStatus.BAD_REQUEST.value(), message, getCurrentTime());
    return ResponseEntity.status(HttpStatus.OK).body(resultEntity);
  }

  public static <T> ResultEntity<T> buildSuccess(Integer code, String message, T data) {
    return new ResultEntity<>(code, message, getCurrentTime(), data);
  }

  public static <T> ResultEntity<T> buildError(Integer code, String message, T data) {
    return new ResultEntity<>(code, message, getCurrentTime(), data);
  }

  public static <T> ResultEntity<T> buildError(Integer code, String message) {
    return new ResultEntity<>(code, message, getCurrentTime());
  }
}
