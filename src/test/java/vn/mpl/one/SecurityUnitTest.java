package vn.mpl.one;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.mpl.one.core.security.*;

class SecurityUnitTest {
  @Test
  void mapsRealmAndApiRolesButIgnoresOtherClients() {
    var jwt =
        Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .subject("s")
            .claim("realm_access", Map.of("roles", List.of("TEACHER")))
            .claim(
                "resource_access",
                Map.of(
                    "mpl-api",
                    Map.of("roles", List.of("ORG_ADMIN")),
                    "other",
                    Map.of("roles", List.of("PLATFORM_ADMIN"))))
            .build();
    assertThat(new KeycloakJwtRoleConverter().convert(jwt))
        .extracting("authority")
        .containsExactlyInAnyOrder("ROLE_TEACHER", "ROLE_ORG_ADMIN");
  }

  @Test
  void platformRoleDoesNotBypassMembership() {
    var repo = mock(TenantMembershipLookup.class);
    var users = mock(CurrentUserProvider.class);
    var org = UUID.randomUUID();
    when(users.current())
        .thenReturn(new AuthenticatedUser("s", "admin", Set.of("ROLE_PLATFORM_ADMIN")));
    when(repo.activeRole(org, "s")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> new TenantAuthorizationService(repo, users).require(org, false))
        .isInstanceOf(AccessDeniedException.class);
  }
}
