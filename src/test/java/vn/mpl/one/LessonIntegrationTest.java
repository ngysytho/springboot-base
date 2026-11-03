package vn.mpl.one;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import vn.mpl.one.modules.lesson.domain.Lesson;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class LessonIntegrationTest {
  @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManagerFactory emf;
  static final String ORG = "11111111-1111-1111-1111-111111111111",
      OTHER = "22222222-2222-2222-2222-222222222222",
      TEACHER = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      STUDENT = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

  org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      teacher() {
    return jwt()
        .jwt(j -> j.subject(TEACHER))
        .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"));
  }

  @BeforeEach
  void clean() {
    jdbc.update("delete from lessons");
    jdbc.update("delete from organization_memberships where organization_id=?::uuid", OTHER);
  }

  String create() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/lessons")
                    .with(teacher())
                    .header("X-Organization-Id", ORG)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Math\",\"description\":\"Algebra\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(response).path("data").path("id").asText();
  }

  @Test
  void missingJwtIs401() throws Exception {
    mvc.perform(get("/api/v1/lessons").header("X-Organization-Id", ORG))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void insufficientRoleIs403() throws Exception {
    mvc.perform(
            post("/api/v1/lessons")
                .with(
                    jwt()
                        .jwt(j -> j.subject(STUDENT))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Math\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void crudAndStaleVersion() throws Exception {
    String id = create();
    mvc.perform(get("/api/v1/lessons/" + id).with(teacher()).header("X-Organization-Id", ORG))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DRAFT"));
    mvc.perform(
            put("/api/v1/lessons/" + id)
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Math advanced\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version").value(1));
    mvc.perform(
            put("/api/v1/lessons/" + id)
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Stale\",\"version\":0}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/lessons/" + id + "/publish")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    mvc.perform(
            delete("/api/v1/lessons/" + id)
                .param("version", "2")
                .with(teacher())
                .header("X-Organization-Id", ORG))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/lessons/" + id).with(teacher()).header("X-Organization-Id", ORG))
        .andExpect(status().isNotFound());
  }

  @Test
  void tenantScopeCannotBeOverridden() throws Exception {
    String id = create();
    mvc.perform(get("/api/v1/lessons/" + id).with(teacher()).header("X-Organization-Id", OTHER))
        .andExpect(status().isForbidden());
    jdbc.update(
        "insert into"
            + " organization_memberships(id,organization_id,user_profile_id,role,active,created_at,updated_at,version)"
            + " values (?,?::uuid,?::uuid,'TEACHER',true,now(),now(),0) on conflict"
            + " (organization_id,user_profile_id) do nothing",
        UUID.randomUUID(),
        OTHER,
        TEACHER);
    mvc.perform(get("/api/v1/lessons/" + id).with(teacher()).header("X-Organization-Id", OTHER))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/lessons/" + id)
                .with(teacher())
                .header("X-Organization-Id", OTHER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Unauthorized\",\"version\":0}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/lessons/" + id + "/publish")
                .with(teacher())
                .header("X-Organization-Id", OTHER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/api/v1/lessons/" + id)
                .with(teacher())
                .header("X-Organization-Id", OTHER)
                .param("version", "0"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/lessons").with(teacher()).header("X-Organization-Id", OTHER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(0));
    mvc.perform(
            get("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .param("filter", "organizationId:eq:" + OTHER))
        .andExpect(status().isBadRequest());
  }

  @Test
  void databaseFilteringAndPagination() throws Exception {
    create();
    mvc.perform(
            get("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .param("filter", "title:like:math", "status:in:DRAFT,PUBLISHED")
                .param("sort", "title:asc")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(1))
        .andExpect(jsonPath("$.data.page").value(1));
    mvc.perform(
            get("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .param("filter", "title:like:%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.count").value(0));
    mvc.perform(
            get("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void realDatabaseOptimisticLockPreventsLostUpdate() throws Exception {
    UUID id = UUID.fromString(create());
    EntityManager a = emf.createEntityManager(), b = emf.createEntityManager();
    try {
      a.getTransaction().begin();
      b.getTransaction().begin();
      Lesson first = a.find(Lesson.class, id), second = b.find(Lesson.class, id);
      first.edit("First", null);
      a.getTransaction().commit();
      second.edit("Second", null);
      assertThatThrownBy(() -> b.getTransaction().commit()).isInstanceOf(RollbackException.class);
    } finally {
      if (a.getTransaction().isActive()) a.getTransaction().rollback();
      if (b.getTransaction().isActive()) b.getTransaction().rollback();
      a.close();
      b.close();
    }
  }

  @Test
  void allFilterOperatorsUsePostgresAndSingleInIsPreserved() throws Exception {
    create();
    mvc.perform(
            post("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Science\"}"))
        .andExpect(status().isCreated());
    Map<String, Integer> cases =
        Map.of(
            "title:eq:Math",
            1,
            "title:neq:Math",
            1,
            "version:gt:0",
            0,
            "version:gte:0",
            2,
            "version:lt:1",
            2,
            "version:lte:0",
            2,
            "title:like:math",
            1,
            "status:in:DRAFT,PUBLISHED",
            2,
            "description:isnull",
            1,
            "description:notnull",
            1);
    for (var entry : cases.entrySet()) {
      mvc.perform(
              get("/api/v1/lessons")
                  .with(teacher())
                  .header("X-Organization-Id", ORG)
                  .param("filter", entry.getKey()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.count").value(entry.getValue()));
    }
    mvc.perform(
            get("/api/v1/lessons")
                .with(teacher())
                .header("X-Organization-Id", ORG)
                .param("filter", "status:eq:INVALID"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void realmRoleCannotOverrideReadOnlyMembership() throws Exception {
    mvc.perform(
            post("/api/v1/lessons")
                .with(
                    jwt()
                        .jwt(j -> j.subject(STUDENT))
                        .authorities(new SimpleGrantedAuthority("ROLE_TEACHER")))
                .header("X-Organization-Id", ORG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Denied\"}"))
        .andExpect(status().isForbidden());
  }
}
