package vn.mpl.one.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import vn.mpl.one.core.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), Objects.toString(e.getDefaultMessage(), "Invalid value")));
    return new ResponseEntity<>(new ErrorResponse(false, 400, "Bad Request", "Validation failed",
        path(request), Instant.now(), errors), headers, HttpStatus.BAD_REQUEST);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    // Preserve framework statuses (400/404/405/415/etc.) without returning framework exception details.
    String message = status.is5xxServerError() ? "Internal server error" : "Invalid request";
    return new ResponseEntity<>(ErrorResponse.of(status.value(), message, path(request)), headers, status);
  }

  @ExceptionHandler({BadRequestException.class, jakarta.validation.ConstraintViolationException.class})
  ResponseEntity<ErrorResponse> bad(Exception ex, HttpServletRequest request) {
    return error(400, "Invalid request", request);
  }
  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ErrorResponse> missing(Exception ex, HttpServletRequest request) {
    return error(404, "Resource not found", request);
  }
  @ExceptionHandler({BusinessException.class, OptimisticLockingFailureException.class,
      jakarta.persistence.OptimisticLockException.class, DataIntegrityViolationException.class})
  ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest request) {
    return error(409, "Resource conflict; reload and retry", request);
  }
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> forbidden(Exception ex, HttpServletRequest request) {
    return error(403, "Access denied", request);
  }
  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> unauthenticated(Exception ex, HttpServletRequest request) {
    return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .body(ErrorResponse.of(401, "Authentication required", request.getRequestURI()));
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
    // Never log request bodies, tokens, credentials, or exception messages containing bind values.
    LOG.error("Unhandled request failure: {}", ex.getClass().getName());
    return error(500, "Internal server error", request);
  }
  private ResponseEntity<ErrorResponse> error(int status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status).body(ErrorResponse.of(status, message, request.getRequestURI()));
  }
  private String path(WebRequest request) {
    return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
  }
}
