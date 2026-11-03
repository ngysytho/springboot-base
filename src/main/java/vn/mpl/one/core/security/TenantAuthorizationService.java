package vn.mpl.one.core.security;

import java.util.*;
import vn.mpl.one.core.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TenantAuthorizationService {
  private final TenantMembershipLookup memberships;
  private final CurrentUserProvider users;

  public TenantAuthorizationService(TenantMembershipLookup memberships, CurrentUserProvider users) {
    this.memberships = memberships;
    this.users = users;
  }

  public TenantContext require(UUID organizationId, boolean write) {
    var user = users.current();
    var member =
        memberships
            .activeRole(organizationId, user.subject())
            .orElseThrow(() -> new AccessDeniedException("Active membership required"));
    if (write && !Set.of("ORG_ADMIN", "TEACHER").contains(member))
      throw new AccessDeniedException("Membership cannot edit lessons");
    return new TenantContext(organizationId, user.subject(), member);
  }
}
