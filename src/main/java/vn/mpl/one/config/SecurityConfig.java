package vn.mpl.one.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;
import vn.mpl.one.core.response.ErrorResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> audience =
        jwt ->
            jwt.getAudience() != null && jwt.getAudience().contains("mpl-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Required audience missing", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audience));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper,
      org.springframework.security.oauth2.client.registration.ClientRegistrationRepository registrations,
      org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository clients,
      JwtDecoder decoder,
      @Value("${app.auth.post-logout-uri:http://localhost:8081/}") String logoutUri) throws Exception {
    var entry = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, e) -> {
      response.setStatus(401);
      response.setContentType("application/json");
      mapper.writeValue(response.getOutputStream(), ErrorResponse.of(401, "Authentication required", request.getRequestURI()));
    };
    var denied = (org.springframework.security.web.access.AccessDeniedHandler) (request, response, e) -> {
      response.setStatus(403);
      response.setContentType("application/json");
      mapper.writeValue(response.getOutputStream(), ErrorResponse.of(403, "Access denied", request.getRequestURI()));
    };
    return http.cors(org.springframework.security.config.Customizer.withDefaults())
        .csrf(c -> c.csrfTokenRepository(new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository())
            .csrfTokenRequestHandler(new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation(f -> f.changeSessionId()))
        .requestCache(c -> c.requestCache(new org.springframework.security.web.savedrequest.NullRequestCache()))
        .authorizeHttpRequests(a -> a.requestMatchers("/", "/error", "/oauth2/authorization/mpl-bff",
            "/login/oauth2/code/mpl-bff", "/api/v1/auth/csrf", "/actuator/health", "/actuator/health/**",
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll().anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
        .oauth2Login(o -> o.authorizedClientRepository(clients)
            .userInfoEndpoint(u -> u.oidcUserService(new vn.mpl.one.core.security.OidcAuthoritiesMapper(decoder)))
            .defaultSuccessUrl("/api/v1/auth/me", true)
            .failureHandler((req, res, ex) -> entry.commence(req, res, ex)))
        .logout(l -> l.logoutUrl("/logout").invalidateHttpSession(true).clearAuthentication(true)
            .deleteCookies("JSESSIONID", "SESSION")
            .logoutSuccessHandler((req, res, auth) -> {
              // Keycloak accepts client_id without id_token_hint, avoiding JWTs in browser URLs.
              var registration = registrations.findByRegistrationId("mpl-bff");
              String endpoint = registration.getProviderDetails().getIssuerUri() + "/protocol/openid-connect/logout";
              res.sendRedirect(org.springframework.web.util.UriComponentsBuilder.fromUriString(endpoint)
                  .queryParam("client_id", registration.getClientId())
                  .queryParam("post_logout_redirect_uri", logoutUri).build().encode().toUriString());
            }))
        .build();
  }
}
