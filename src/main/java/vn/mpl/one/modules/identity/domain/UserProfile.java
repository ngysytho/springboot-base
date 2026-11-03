package vn.mpl.one.modules.identity.domain;

import jakarta.persistence.*;
import vn.mpl.one.core.entity.BaseEntity;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String subject;

  @Column(nullable = false)
  private String displayName;

  protected UserProfile() {}

  public UserProfile(String subject, String displayName) {
    this.subject = subject;
    this.displayName = displayName;
  }

  public String getSubject() {
    return subject;
  }
}
