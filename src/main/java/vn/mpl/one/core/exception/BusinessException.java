package vn.mpl.one.core.exception;

/** Expected business-rule failure, safe to represent as HTTP 409. */
public class BusinessException extends RuntimeException {
  public BusinessException(String message) { super(message); }
}
