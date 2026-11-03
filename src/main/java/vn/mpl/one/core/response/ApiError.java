package vn.mpl.one.core.response;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    boolean success, int status, String message, Map<String, String> errors, Instant timestamp) {
  public static ApiError of(int status, String message) {
    return new ApiError(false, status, message, Map.of(), Instant.now());
  }
}
