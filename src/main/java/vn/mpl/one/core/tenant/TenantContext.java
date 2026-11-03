package vn.mpl.one.core.tenant;

import java.util.UUID;

/** Immutable explicit context, constructed only after application membership authorization. */
public record TenantContext(UUID organizationId, String subject, String membershipRole) {}
