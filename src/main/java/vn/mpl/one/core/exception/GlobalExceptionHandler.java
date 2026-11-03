package vn.mpl.one.core.exception;

import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.mpl.one.core.response.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError(false, 400, "Validation failed", errors, Instant.now()));
  }

  @ExceptionHandler({
    BadRequestException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MissingRequestHeaderException.class,
    MissingServletRequestParameterException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  ResponseEntity<ApiError> bad(Exception e) {
    return error(400, "Invalid request");
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> missing(Exception e) {
    return error(404, "Resource not found");
  }

  @ExceptionHandler({
    ConflictException.class,
    OptimisticLockingFailureException.class,
    DataIntegrityViolationException.class
  })
  ResponseEntity<ApiError> conflict(Exception e) {
    return error(409, "Resource conflict; reload and retry");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> forbidden(Exception e) {
    return error(403, "Access denied");
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ApiError> unauthenticated(Exception e) {
    return error(401, "Authentication required");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    log.error("Unhandled request failure", e);
    return error(500, "Internal server error");
  }

  private ResponseEntity<ApiError> error(int status, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status, message));
  }
}
