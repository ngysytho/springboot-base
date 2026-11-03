package vn.mpl.one.modules.lesson.api.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import vn.mpl.one.core.filter.FilterParser;
import vn.mpl.one.core.pagination.*;
import vn.mpl.one.core.response.ApiResponse;
import vn.mpl.one.modules.lesson.api.dto.request.*;
import vn.mpl.one.modules.lesson.api.dto.response.LessonResponse;
import vn.mpl.one.modules.lesson.api.mapper.LessonApiMapper;
import vn.mpl.one.modules.lesson.application.service.LessonService;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
  private final LessonService service;
  private final LessonApiMapper mapper;

  public LessonController(LessonService service, LessonApiMapper mapper) {
    this.service = service; this.mapper = mapper;
  }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<LessonResponse> create(@RequestHeader("X-Organization-Id") UUID org,
      @Valid @RequestBody CreateLessonRequest body) {
    return ApiResponse.ok(mapper.toResponse(service.create(org, mapper.toCommand(body))));
  }
  @GetMapping
  public ApiResponse<PageResponse<LessonResponse>> list(@RequestHeader("X-Organization-Id") UUID org,
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam MultiValueMap<String, String> query) {
    var result = service.list(org, new PaginationRequest(page, size, query.get("sort")), FilterParser.parse(query.get("filter")));
    return ApiResponse.ok(result.map(mapper::toResponse));
  }
  @GetMapping("/{id}")
  public ApiResponse<LessonResponse> get(@RequestHeader("X-Organization-Id") UUID org, @PathVariable UUID id) {
    return ApiResponse.ok(mapper.toResponse(service.get(org, id)));
  }
  @PutMapping("/{id}")
  public ApiResponse<LessonResponse> update(@RequestHeader("X-Organization-Id") UUID org, @PathVariable UUID id,
      @Valid @RequestBody UpdateLessonRequest body) {
    return ApiResponse.ok(mapper.toResponse(service.update(org, id, mapper.toCommand(body))));
  }
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@RequestHeader("X-Organization-Id") UUID org, @PathVariable UUID id,
      @RequestParam Long version) {
    service.delete(org, id, version);
    return ApiResponse.ok(null);
  }
  @PostMapping("/{id}/publish")
  public ApiResponse<LessonResponse> publish(@RequestHeader("X-Organization-Id") UUID org, @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body) {
    return ApiResponse.ok(mapper.toResponse(service.publish(org, id, body.version())));
  }
}
