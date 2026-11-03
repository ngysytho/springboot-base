package vn.mpl.one.modules.lesson.domain;

import jakarta.persistence.*;
import java.util.UUID;
import vn.mpl.one.core.entity.*;

@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity implements TenantOwned {
  @Column(nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 10000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LessonStatus status = LessonStatus.DRAFT;

  protected Lesson() {}

  public Lesson(UUID organizationId, String title, String description) {
    this.organizationId = organizationId;
    edit(title, description);
  }

  public void edit(String title, String description) {
    this.title = title;
    this.description = description;
  }

  public void publish() {
    if (status == LessonStatus.ARCHIVED)
      throw new vn.mpl.one.core.exception.ConflictException("Archived lessons cannot be published");
    status = LessonStatus.PUBLISHED;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LessonStatus getStatus() {
    return status;
  }
}
