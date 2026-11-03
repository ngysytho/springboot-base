package vn.mpl.one.modules.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import vn.mpl.one.core.exception.BusinessException;
import vn.mpl.one.core.exception.ConflictException;
import vn.mpl.one.core.tenant.TenantOwned;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;

/** Immutable domain state; no persistence or HTTP framework dependency. */
public record Lesson(UUID id, UUID organizationId, String title, String description,
    LessonStatus status, Instant createdAt, Instant updatedAt, Long version) implements TenantOwned {
  public Lesson {
    Objects.requireNonNull(organizationId, "organizationId");
    Objects.requireNonNull(status, "status");
    if (title == null || title.isBlank() || title.length() > 200)
      throw new BusinessException("Lesson title must contain 1 to 200 characters");
    if (description != null && description.length() > 10000)
      throw new BusinessException("Lesson description exceeds 10000 characters");
  }

  public static Lesson draft(UUID organizationId, String title, String description) {
    return new Lesson(null, organizationId, title, description, LessonStatus.DRAFT, null, null, null);
  }
  public Lesson edit(String title, String description) {
    return new Lesson(id, organizationId, title, description, status, createdAt, updatedAt, version);
  }
  public Lesson publish() {
    if (status == LessonStatus.ARCHIVED) throw new ConflictException("Archived lessons cannot be published");
    return new Lesson(id, organizationId, title, description, LessonStatus.PUBLISHED, createdAt, updatedAt, version);
  }
  public void requireVersion(Long expected) {
    if (expected == null || !Objects.equals(version, expected)) throw new ConflictException("Stale version");
  }
  @Override public UUID getOrganizationId() { return organizationId; }
}
