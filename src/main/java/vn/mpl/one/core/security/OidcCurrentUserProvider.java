package vn.mpl.one.core.security;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
@Component
public class OidcCurrentUserProvider implements CurrentUserProvider {
  public AuthenticatedUser current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof OidcUser user))
      throw new AuthenticationCredentialsNotFoundException("OIDC session required");
    return new AuthenticatedUser(user.getSubject(), user.getPreferredUsername(), user.getFullName(),
        auth.getAuthorities().stream().map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_")).collect(Collectors.toUnmodifiableSet()));
  }
}
