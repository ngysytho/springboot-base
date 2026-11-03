package vn.mpl.one;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;
import vn.mpl.one.core.exception.ConflictException;
import vn.mpl.one.core.security.TenantAuthorizationService;
import vn.mpl.one.core.tenant.TenantContext;
import vn.mpl.one.modules.lesson.application.service.LessonService;
import vn.mpl.one.modules.lesson.application.usecase.*;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;
import vn.mpl.one.modules.lesson.domain.repository.LessonRepository;

class LessonServiceTest {
  private final UUID org = UUID.randomUUID(), id = UUID.randomUUID();
  private LessonRepository repository;
  private TenantAuthorizationService tenants;
  private LessonService service;
  @BeforeEach void setup() {
    repository = mock(LessonRepository.class); tenants = mock(TenantAuthorizationService.class);
    service = new LessonService(repository, tenants);
    when(tenants.require(org, true)).thenReturn(new TenantContext(org, "teacher", "TEACHER"));
  }
  Lesson lesson(LessonStatus status) { return new Lesson(id, org, "Math", null, status, Instant.now(), Instant.now(), 2L); }
  @Test void authorizationPrecedesPersistence() {
    when(tenants.require(org, true)).thenThrow(new AccessDeniedException("Denied"));
    assertThatThrownBy(() -> service.create(org, new CreateLessonCommand("Math", null))).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(repository);
  }
  @Test void createUsesAuthorizedTenantAndDomainDraft() {
    when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    Lesson created = service.create(org, new CreateLessonCommand("Math", null));
    assertThat(created.organizationId()).isEqualTo(org);
    assertThat(created.status()).isEqualTo(LessonStatus.DRAFT);
  }
  @Test void staleUpdateDoesNotWrite() {
    when(repository.findById(org, id)).thenReturn(Optional.of(lesson(LessonStatus.DRAFT)));
    assertThatThrownBy(() -> service.update(org, id, new UpdateLessonCommand("New", null, 1L))).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }
  @Test void publishingUsesDomainRule() {
    when(repository.findById(org, id)).thenReturn(Optional.of(lesson(LessonStatus.ARCHIVED)));
    assertThatThrownBy(() -> service.publish(org, id, 2L)).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }
  @Test void publishPersistsPublishedState() {
    when(repository.findById(org, id)).thenReturn(Optional.of(lesson(LessonStatus.DRAFT)));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.publish(org, id, 2L).status()).isEqualTo(LessonStatus.PUBLISHED);
  }
}
