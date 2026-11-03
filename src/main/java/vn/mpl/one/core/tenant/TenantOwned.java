package vn.mpl.one.core.tenant;

import java.util.UUID;

public interface TenantOwned {
  UUID getOrganizationId();
}
