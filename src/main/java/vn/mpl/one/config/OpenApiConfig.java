package vn.mpl.one.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI openApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
    var oauth =
        new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .flows(
                new OAuthFlows()
                    .authorizationCode(
                        new OAuthFlow()
                            .authorizationUrl(issuer + "/protocol/openid-connect/auth")
                            .tokenUrl(issuer + "/protocol/openid-connect/token")
                            .scopes(
                                new Scopes()
                                    .addString("openid", "Identity")
                                    .addString("profile", "Profile"))));
    return new OpenAPI()
        .info(new Info().title("MPL One API").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes("keycloak", oauth))
        .addSecurityItem(new SecurityRequirement().addList("bearer"))
        .addSecurityItem(new SecurityRequirement().addList("keycloak"));
  }
}
