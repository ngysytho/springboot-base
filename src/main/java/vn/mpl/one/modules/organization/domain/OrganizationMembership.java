package vn.mpl.one.modules.organization.domain;

import jakarta.persistence.*;
import java.util.UUID;
import vn.mpl.one.core.entity.*;

@Entity
@Table(
    name = "organization_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_profile_id"}))
public class OrganizationMembership extends BaseEntity implements TenantOwned {
  @Column(nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, updatable = false)
  private UUID userProfileId;

  @Column(nullable = false)
  private String role;

  @Column(nullable = false)
  private boolean active;

  protected OrganizationMembership() {}

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getRole() {
    return role;
  }
}
