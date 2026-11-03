package vn.mpl.one.core.security;

import java.util.Set;

public record AuthenticatedUser(String subject, String username, Set<String> roles) {}
