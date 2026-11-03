package vn.mpl.one.modules.lesson.api;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.mpl.one.core.pagination.PaginationRequest;
import vn.mpl.one.core.response.*;
import vn.mpl.one.modules.lesson.application.LessonService;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
  private final LessonService service;

  public LessonController(LessonService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<LessonResponse> create(
      @RequestHeader("X-Organization-Id") UUID org, @Valid @RequestBody CreateLessonRequest body) {
    return ApiResponse.ok(service.create(org, body));
  }

  @GetMapping
  public ApiResponse<PageResponse<LessonResponse>> list(
      @RequestHeader("X-Organization-Id") UUID org,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) List<String> sort,
      @RequestParam org.springframework.util.MultiValueMap<String, String> query) {
    return ApiResponse.ok(
        service.list(org, new PaginationRequest(page, size, sort), query.get("filter")));
  }

  @GetMapping("/{id}")
  public ApiResponse<LessonResponse> get(
      @RequestHeader("X-Organization-Id") UUID org, @PathVariable UUID id) {
    return ApiResponse.ok(service.get(org, id));
  }

  @PutMapping("/{id}")
  public ApiResponse<LessonResponse> update(
      @RequestHeader("X-Organization-Id") UUID org,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateLessonRequest body) {
    return ApiResponse.ok(service.update(org, id, body));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader("X-Organization-Id") UUID org,
      @PathVariable UUID id,
      @RequestParam Long version) {
    service.delete(org, id, version);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/publish")
  public ApiResponse<LessonResponse> publish(
      @RequestHeader("X-Organization-Id") UUID org,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body) {
    return ApiResponse.ok(service.publish(org, id, body.version()));
  }
}
