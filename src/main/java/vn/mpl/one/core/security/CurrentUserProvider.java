package vn.mpl.one.core.security;

import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
  public AuthenticatedUser current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwt))
      throw new AuthenticationCredentialsNotFoundException("JWT required");
    return new AuthenticatedUser(
        jwt.getToken().getSubject(),
        jwt.getToken().getClaimAsString("preferred_username"),
        jwt.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .collect(Collectors.toUnmodifiableSet()));
  }
}
