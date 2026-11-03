package vn.mpl.one.modules.organization.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mpl.one.core.repository.BaseRepository;
import vn.mpl.one.modules.organization.domain.OrganizationMembership;

public interface MembershipRepository extends BaseRepository<OrganizationMembership, UUID> {
  @Query(
      "select m from OrganizationMembership m, UserProfile u where m.userProfileId=u.id and"
          + " m.organizationId=:org and u.subject=:subject and m.active=true")
  Optional<OrganizationMembership> findActive(
      @Param("org") UUID organizationId, @Param("subject") String subject);
}
