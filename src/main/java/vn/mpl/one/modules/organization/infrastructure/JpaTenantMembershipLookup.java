package vn.mpl.one.modules.organization.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.mpl.one.core.security.TenantMembershipLookup;
import vn.mpl.one.modules.organization.domain.OrganizationMembership;

@Component
public class JpaTenantMembershipLookup implements TenantMembershipLookup {
  private final MembershipRepository memberships;

  public JpaTenantMembershipLookup(MembershipRepository memberships) {
    this.memberships = memberships;
  }

  @Override
  public Optional<String> activeRole(UUID organizationId, String subject) {
    return memberships.findActive(organizationId, subject).map(OrganizationMembership::getRole);
  }
}
