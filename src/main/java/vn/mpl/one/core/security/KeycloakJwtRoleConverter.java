package vn.mpl.one.core.security;

import java.util.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<GrantedAuthority> result = new HashSet<>();
    add(jwt.getClaim("realm_access"), result);
    Object access = jwt.getClaim("resource_access");
    if (access instanceof Map<?, ?> clients) add(clients.get("mpl-api"), result);
    return result;
  }

  private void add(Object access, Set<GrantedAuthority> out) {
    if (access instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles)
      for (Object role : roles)
        if (role instanceof String r) out.add(new SimpleGrantedAuthority("ROLE_" + r));
  }
}
