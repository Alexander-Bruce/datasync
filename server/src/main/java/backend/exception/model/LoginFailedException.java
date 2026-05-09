package backend.exception.model;

public class LoginFailedException extends BaseException {
  public LoginFailedException() {
    super("Error in username or password", 401);
  }
}
