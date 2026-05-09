package backend.exception;

import backend.exception.model.BaseException;
import backend.util.ResultEntity;
import jakarta.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ResultEntity<Object>> exceptionHandler(BaseException ex) {
    return ResultEntity.error(ex.getCode(), ex.getMessage());
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<Object>> exceptionHandler(
      SQLIntegrityConstraintViolationException ex) {
    return ResultEntity.error(ex.getMessage());
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    String message = "Validation error: " + ex.getMessage();
    int startIndex = message.lastIndexOf("message [") + "message [".length();
    int endIndex = message.lastIndexOf("]") - 1;

    return ResultEntity.error(
        HttpStatus.FORBIDDEN.value(), message.substring(startIndex, endIndex).trim());
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<String>> handleConstraintViolation(
      ConstraintViolationException ex) {
    StringBuilder message = new StringBuilder("Constraint violation: ");
    ex.getConstraintViolations()
        .forEach(
            violation ->
                message
                    .append(violation.getPropertyPath())
                    .append(": ")
                    .append(violation.getMessage())
                    .append("; "));
    return ResultEntity.error(HttpStatus.BAD_REQUEST.value(), message.toString());
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<String>> handleClassCastException(ClassCastException ex) {
    return ResultEntity.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<Object>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    String message =
        "Invalid format: " + (ex.getMessage() != null ? ex.getMessage() : "No message available");
    return ResultEntity.error(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<Object>> handleAccessDeniedException(
      AccessDeniedException ex) {
    String errorMessage = "The requested resource was not found: " + ex.getMessage();
    return ResultEntity.error(HttpStatus.NOT_FOUND.value(), errorMessage);
  }

  //    @ExceptionHandler
  //    @ResponseBody
  //    public ResponseEntity<ResultEntity<Object>>
  // handleAuthenticationException(AuthenticationException ex) {
  //        return ResultEntity.error(HttpStatus.NOT_FOUND.value(), "filter error: " +
  // ex.getMessage());
  //    }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<Object>> handleRuntimeException(RuntimeException ex) {
    return ResultEntity.error(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        ex.getMessage() != null ? ex.getMessage() : "Internal server error");
  }

  @ExceptionHandler
  public ResponseEntity<ResultEntity<Object>> noHandleFoundException(NoHandlerFoundException ex) {
    return ResultEntity.error(
        HttpStatus.NOT_FOUND.value(), "No handler found for " + ex.getMessage());
  }
}
