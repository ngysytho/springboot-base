package vn.mpl.one.config;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.client.web.*;
@Configuration
public class OAuth2ClientConfig {
  @Bean OAuth2AuthorizedClientRepository authorizedClients() {
    return new HttpSessionOAuth2AuthorizedClientRepository();
  }
}
