package vn.mpl.one.modules.lesson.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;
import vn.mpl.one.core.entity.BaseEntity;
import vn.mpl.one.core.tenant.TenantOwned;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;

@Entity(name = "Lesson")
@Table(name = "lessons")
public class LessonJpaEntity extends BaseEntity implements TenantOwned {
  @Column(nullable = false, updatable = false) private UUID organizationId;
  @Column(nullable = false, length = 200) private String title;
  @Column(length = 10000) private String description;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LessonStatus status;

  protected LessonJpaEntity() {}
  public LessonJpaEntity(UUID organizationId, String title, String description, LessonStatus status) {
    this.organizationId = organizationId;
    updateContent(title, description, status);
  }
  public void updateContent(String title, String description, LessonStatus status) {
    this.title = title; this.description = description; this.status = status;
  }
  @Override public UUID getOrganizationId() { return organizationId; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public LessonStatus getStatus() { return status; }
}
