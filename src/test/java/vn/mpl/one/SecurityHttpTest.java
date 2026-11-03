package vn.mpl.one;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mpl.one.config.SecurityConfig;
import vn.mpl.one.core.security.CurrentUserProvider;
import vn.mpl.one.modules.identity.api.MeController;

@WebMvcTest(MeController.class)
@ContextConfiguration(
    classes = {MeController.class, SecurityConfig.class, CurrentUserProvider.class})
class SecurityHttpTest {
  @Autowired MockMvc mvc;
  @MockitoBean JwtDecoder decoder;

  @Test
  void missingJwtReturnsStructured401() throws Exception {
    mvc.perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(header().string("WWW-Authenticate", "Bearer"));
  }

  @Test
  void meUsesAuthenticatedSubject() throws Exception {
    mvc.perform(
            get("/api/v1/me")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.subject("known-subject").claim("preferred_username", "teacher"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.subject").value("known-subject"));
  }
}
