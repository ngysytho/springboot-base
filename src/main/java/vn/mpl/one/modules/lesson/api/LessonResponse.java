package vn.mpl.one.modules.lesson.api;

import java.time.Instant;
import java.util.UUID;
import vn.mpl.one.modules.lesson.domain.*;

public record LessonResponse(
    UUID id,
    UUID organizationId,
    String title,
    String description,
    LessonStatus status,
    Instant createdAt,
    Instant updatedAt,
    Long version) {
  public static LessonResponse from(Lesson e) {
    return new LessonResponse(
        e.getId(),
        e.getOrganizationId(),
        e.getTitle(),
        e.getDescription(),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }
}
