package vn.mpl.one.modules.organization.infrastructure.persistence.entity;

import jakarta.persistence.*;
import vn.mpl.one.core.entity.BaseEntity;

@Entity(name = "Organization")
@Table(name = "organizations")
public class OrganizationJpaEntity extends BaseEntity {
  @Column(nullable = false, length = 200)
  private String name;

  protected OrganizationJpaEntity() {}

  public OrganizationJpaEntity(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
