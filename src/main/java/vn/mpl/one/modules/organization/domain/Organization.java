package vn.mpl.one.modules.organization.domain;

import jakarta.persistence.*;
import vn.mpl.one.core.entity.BaseEntity;

@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {
  @Column(nullable = false, length = 200)
  private String name;

  protected Organization() {}

  public Organization(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
