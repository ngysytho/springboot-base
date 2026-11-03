package vn.mpl.one.modules.organization.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.mpl.one.core.security.TenantMembershipLookup;
import vn.mpl.one.modules.organization.infrastructure.persistence.entity.OrganizationMembershipJpaEntity;

@Component
public class JpaTenantMembershipLookup implements TenantMembershipLookup {
  private final MembershipJpaRepository memberships;

  public JpaTenantMembershipLookup(MembershipJpaRepository memberships) {
    this.memberships = memberships;
  }

  @Override
  public Optional<String> activeRole(UUID organizationId, String subject) {
    return memberships.findActive(organizationId, subject).map(OrganizationMembershipJpaEntity::getRole);
  }
}
