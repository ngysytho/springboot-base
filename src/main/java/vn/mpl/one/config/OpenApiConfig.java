package vn.mpl.one.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;
@Configuration
public class OpenApiConfig {
  @Bean OpenAPI openApi() {
    return new OpenAPI().info(new Info().title("MPL One BFF API").version("v1"))
        .components(new Components().addSecuritySchemes("session", new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
        .addSecurityItem(new SecurityRequirement().addList("session"));
  }
}
