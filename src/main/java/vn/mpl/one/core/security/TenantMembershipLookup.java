package vn.mpl.one.core.security;

import java.util.Optional;
import java.util.UUID;

/** Application-data membership port; identity-provider claims alone cannot satisfy it. */
public interface TenantMembershipLookup {
  Optional<String> activeRole(UUID organizationId, String subject);
}
