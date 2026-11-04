package vn.mpl.one.core.security;
import java.util.Set;
import org.springframework.security.oauth2.client.oidc.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
/** Roles come only from a signature, issuer, expiry and audience validated access token. */
public class OidcAuthoritiesMapper extends OidcUserService {
  private static final Set<String> ROLES = Set.of("ROLE_PLATFORM_ADMIN", "ROLE_ORG_ADMIN",
      "ROLE_TEACHER", "ROLE_STUDENT", "ROLE_PARENT", "ROLE_BUSINESS_MANAGER");
  private final JwtDecoder decoder;
  public OidcAuthoritiesMapper(JwtDecoder decoder) { this.decoder = decoder; }
  @Override public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
    var user = super.loadUser(request);
    try {
      var jwt = decoder.decode(request.getAccessToken().getTokenValue());
      if (!user.getSubject().equals(jwt.getSubject()))
        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
      var roles = new KeycloakJwtRoleConverter().convert(jwt).stream()
          .filter(a -> ROLES.contains(a.getAuthority())).toList();
      return new DefaultOidcUser(roles, user.getIdToken(), user.getUserInfo(), "sub");
    } catch (org.springframework.security.oauth2.jwt.JwtException ex) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Invalid role token");
    }
  }
}
