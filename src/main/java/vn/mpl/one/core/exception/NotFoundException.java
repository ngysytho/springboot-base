package vn.mpl.one.core.exception;

/** Compatibility alias for internal extensions written before the package refactor. */
@Deprecated
public class NotFoundException extends ResourceNotFoundException {
  public NotFoundException(String message) { super(message); }
}
