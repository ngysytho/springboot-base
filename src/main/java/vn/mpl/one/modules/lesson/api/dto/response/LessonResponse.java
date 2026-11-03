package vn.mpl.one.modules.lesson.api.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;

public record LessonResponse(UUID id, UUID organizationId, String title, String description,
    LessonStatus status, Instant createdAt, Instant updatedAt, Long version) {}
