package vn.mpl.one;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import vn.mpl.one.config.*;
import vn.mpl.one.core.exception.GlobalExceptionHandler;
import vn.mpl.one.modules.lesson.api.controller.LessonController;
import vn.mpl.one.modules.lesson.api.mapper.LessonApiMapper;
import vn.mpl.one.modules.lesson.application.service.LessonService;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;

@WebMvcTest(LessonController.class)
@ContextConfiguration(classes = {LessonController.class, LessonApiMapper.class, GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class})
class LessonControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean LessonService service;
  @MockitoBean JwtDecoder decoder;
  private final UUID org = UUID.randomUUID();

  @Test void invalidBodyIs400AndNeverReachesUseCase() throws Exception {
    mvc.perform(post("/api/v1/lessons").with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TEACHER"))).header("X-Organization-Id", org)
        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/lessons"));
    verifyNoInteractions(service);
  }
  @Test void mapsDomainToResponseAndPreservesCreatedStatus() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.create(eq(org), any())).thenReturn(new Lesson(id, org, "Math", null,
        LessonStatus.DRAFT, Instant.now(), Instant.now(), 0L));
    mvc.perform(post("/api/v1/lessons").with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TEACHER"))).header("X-Organization-Id", org)
        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Math\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(id.toString()))
        .andExpect(jsonPath("$.data.organizationId").value(org.toString()));
  }
  @Test void malformedQueryIs400BeforeUseCase() throws Exception {
    mvc.perform(get("/api/v1/lessons").with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TEACHER"))).header("X-Organization-Id", org).param("page", "0"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
