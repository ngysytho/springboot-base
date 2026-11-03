package vn.mpl.one.modules.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import vn.mpl.one.core.entity.BaseEntity;

@Entity(name = "UserProfile")
@Table(name = "user_profiles")
public class UserProfileJpaEntity extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String subject;

  @Column(nullable = false)
  private String displayName;

  protected UserProfileJpaEntity() {}

  public UserProfileJpaEntity(String subject, String displayName) {
    this.subject = subject;
    this.displayName = displayName;
  }

  public String getSubject() {
    return subject;
  }
}
