package vn.mpl.one.modules.organization.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface MembershipManagement {
  void grant(java.util.UUID organizationId, java.util.UUID userProfileId, String role);

  void revoke(java.util.UUID organizationId, java.util.UUID userProfileId);
}
