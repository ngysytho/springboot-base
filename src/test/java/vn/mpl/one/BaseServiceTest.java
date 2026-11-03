package vn.mpl.one;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.exception.*;
import vn.mpl.one.core.service.BaseService;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;
import vn.mpl.one.modules.lesson.infrastructure.persistence.entity.LessonJpaEntity;
import vn.mpl.one.modules.lesson.infrastructure.persistence.repository.LessonJpaRepository;

class BaseServiceTest {
  private LessonJpaRepository repository;
  private PersistenceHarness service;
  private final UUID organization = UUID.randomUUID();
  private final Specification<LessonJpaEntity> scope = (r, q, cb) -> cb.equal(r.get("organizationId"), organization);

  static class PersistenceHarness extends BaseService<LessonJpaEntity> {
    PersistenceHarness(LessonJpaRepository repository) { super(repository); }
    LessonJpaEntity one(Specification<LessonJpaEntity> scope) { return findOneOrFailByCriteria(scope); }
    Page<LessonJpaEntity> page(Specification<LessonJpaEntity> scope, Pageable page) { return paginate(scope, page); }
    void remove(Specification<LessonJpaEntity> scope) { delete(scope); }
    LessonJpaEntity persist(LessonJpaEntity entity, Specification<LessonJpaEntity> scope, UUID organization) { return save(entity, scope, organization); }
    List<LessonJpaEntity> batch(List<LessonJpaEntity> entities, Specification<LessonJpaEntity> scope, UUID organization) { return bulkCreate(entities, scope, organization); }
    long total(Specification<LessonJpaEntity> scope) { return count(scope); }
    boolean contains(Specification<LessonJpaEntity> scope) { return exists(scope); }
  }
  @BeforeEach void setup() { repository = mock(LessonJpaRepository.class); service = new PersistenceHarness(repository); }
  @Test void missingScopedResourceFails() {
    when(repository.findOne(scope)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.one(scope)).isInstanceOf(ResourceNotFoundException.class);
  }
  @Test void deleteMissingDoesNotMutate() {
    when(repository.findOne(scope)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.remove(scope)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).delete(any(LessonJpaEntity.class));
  }
  @Test void scopeIsRequired() {
    assertThatThrownBy(() -> service.total(null)).isInstanceOf(NullPointerException.class);
    verifyNoInteractions(repository);
  }
  @Test void refusesCrossTenantWritesBeforePersistence() {
    var entity = new LessonJpaEntity(UUID.randomUUID(), "Math", null, LessonStatus.DRAFT);
    assertThatThrownBy(() -> service.persist(entity, scope, organization)).isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(repository);
  }
  @Test void paginationAndExistenceRetainScope() {
    var page = PageRequest.of(1, 10);
    when(repository.findAll(scope, page)).thenReturn(Page.empty(page));
    when(repository.exists(scope)).thenReturn(true);
    when(repository.count(scope)).thenReturn(3L);
    assertThat(service.page(scope, page).getNumber()).isEqualTo(1);
    assertThat(service.contains(scope)).isTrue();
    assertThat(service.total(scope)).isEqualTo(3);
  }
  @Test void bulkCreateUsesEntityLifecycle() {
    var entity = new LessonJpaEntity(organization, "Math", null, LessonStatus.DRAFT);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    assertThat(service.batch(List.of(entity), scope, organization)).containsExactly(entity);
    verify(repository).saveAndFlush(entity);
  }
}
