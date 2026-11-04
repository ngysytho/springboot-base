package vn.mpl.one.core.security;
import java.util.Set;
public record AuthenticatedUser(String subject, String username, String displayName, Set<String> roles) {
  public AuthenticatedUser(String subject, String username, Set<String> roles) {
    this(subject, username, username, roles);
  }
}
