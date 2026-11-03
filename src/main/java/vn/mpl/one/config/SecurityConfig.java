package vn.mpl.one.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;
import vn.mpl.one.core.response.ApiError;
import vn.mpl.one.core.security.KeycloakJwtRoleConverter;

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
  SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRoleConverter());
    var entry =
        (org.springframework.security.web.AuthenticationEntryPoint)
            (request, response, e) -> {
              response.setStatus(401);
              response.setHeader("WWW-Authenticate", "Bearer");
              response.setContentType("application/json");
              mapper.writeValue(
                  response.getOutputStream(), ApiError.of(401, "Authentication required"));
            };
    var denied =
        (org.springframework.security.web.access.AccessDeniedHandler)
            (request, response, e) -> {
              response.setStatus(403);
              response.setContentType("application/json");
              mapper.writeValue(response.getOutputStream(), ApiError.of(403, "Access denied"));
            };
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint(entry)
                    .accessDeniedHandler(denied)
                    .jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }
}
