package backend.exception.model;

public class FileDeletedException extends BaseException {

  public FileDeletedException() {
    super("Failed to delete the file", 401);
  }
}
