package vn.mpl.one.modules.lesson.infrastructure.persistence.repository;

import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.exception.*;
import vn.mpl.one.core.filter.FilterCriteria;
import vn.mpl.one.core.pagination.*;
import vn.mpl.one.core.service.BaseService;
import vn.mpl.one.core.specification.SpecificationBuilder;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.domain.repository.LessonRepository;
import vn.mpl.one.modules.lesson.infrastructure.persistence.entity.LessonJpaEntity;
import vn.mpl.one.modules.lesson.infrastructure.persistence.mapper.LessonPersistenceMapper;
import vn.mpl.one.modules.lesson.infrastructure.specification.LessonSpecifications;

@Repository
@Transactional(readOnly = true)
public class LessonRepositoryAdapter extends BaseService<LessonJpaEntity> implements LessonRepository {
  private final LessonPersistenceMapper mapper;

  public LessonRepositoryAdapter(LessonJpaRepository repository, LessonPersistenceMapper mapper) {
    super(repository); this.mapper = mapper;
  }
  @Override public Optional<Lesson> findById(UUID organizationId, UUID id) {
    return super.findById(id, LessonSpecifications.tenant(organizationId)).map(mapper::toDomain);
  }
  @Override public PageResponse<Lesson> findByCriteria(UUID organizationId, FilterCriteria filters, PaginationRequest page) {
    Specification<LessonJpaEntity> userFilters = SpecificationBuilder.build(filters, LessonSpecifications.FILTERS);
    var scoped = LessonSpecifications.tenant(organizationId).and(userFilters);
    return PaginationUtils.from(paginate(scoped, PaginationUtils.pageable(page, LessonSpecifications.SORTS)).map(mapper::toDomain));
  }
  @Override @Transactional public Lesson create(Lesson lesson) {
    if (lesson.id() != null) throw new ConflictException("Entity already exists");
    var scope = LessonSpecifications.tenant(lesson.organizationId());
    return mapper.toDomain(super.create(mapper.toNewEntity(lesson), scope, lesson.organizationId()));
  }
  @Override @Transactional public Lesson save(Lesson lesson) {
    if (lesson.id() == null) throw new ConflictException("Persisted lesson ID required");
    var scope = LessonSpecifications.tenant(lesson.organizationId()).and(LessonSpecifications.id(lesson.id()));
    LessonJpaEntity entity = findOneOrFailByCriteria(scope);
    if (!Objects.equals(entity.getVersion(), lesson.version())) throw new ConflictException("Stale version");
    mapper.copyContent(lesson, entity);
    return mapper.toDomain(super.save(entity, scope, lesson.organizationId()));
  }
  @Override @Transactional public void delete(UUID organizationId, UUID id, Long expectedVersion) {
    var scope = LessonSpecifications.tenant(organizationId).and(LessonSpecifications.id(id));
    LessonJpaEntity entity = findOneOrFailByCriteria(scope);
    if (expectedVersion == null || !Objects.equals(entity.getVersion(), expectedVersion)) throw new ConflictException("Stale version");
    super.delete(scope);
  }
}
