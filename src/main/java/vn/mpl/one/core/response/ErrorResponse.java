package vn.mpl.one.core.response;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Stable error envelope. `errors` retains field-validation details for existing clients. */
public record ErrorResponse(boolean success, int status, String error, String message,
    String path, Instant timestamp, Map<String, String> errors) {
  public ErrorResponse { errors = Map.copyOf(errors); }

  public static ErrorResponse of(int status, String message, String path) {
    HttpStatus known = HttpStatus.resolve(status);
    return new ErrorResponse(false, status, known == null ? "HTTP error" : known.getReasonPhrase(),
        message, path, Instant.now(), Map.of());
  }
}
