package vn.mpl.one.modules.lesson.application;

import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mpl.one.core.exception.ConflictException;
import vn.mpl.one.core.filter.FilterParser;
import vn.mpl.one.core.pagination.PaginationRequest;
import vn.mpl.one.core.response.PageResponse;
import vn.mpl.one.core.security.*;
import vn.mpl.one.core.service.BaseService;
import vn.mpl.one.core.specification.SpecificationBuilder;
import vn.mpl.one.modules.lesson.api.*;
import vn.mpl.one.modules.lesson.domain.*;
import vn.mpl.one.modules.lesson.infrastructure.LessonRepository;

@Service
@Transactional(readOnly = true)
@PreAuthorize(
    "hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN','TEACHER','STUDENT','PARENT','BUSINESS_MANAGER')")
public class LessonService {
  private final BaseService<Lesson> core;
  private final TenantAuthorizationService tenants;

  public LessonService(LessonRepository repository, TenantAuthorizationService tenants) {
    this.core = new BaseService<>(repository);
    this.tenants = tenants;
  }

  private Specification<Lesson> scope(UUID org, UUID id, boolean write) {
    return LessonSpecifications.tenant(tenants.require(org, write))
        .and(LessonSpecifications.id(id));
  }

  public LessonResponse get(UUID org, UUID id) {
    return LessonResponse.from(core.findOneOrFailByCriteria(scope(org, id, false)));
  }

  public PageResponse<LessonResponse> list(UUID org, PaginationRequest page, List<String> filters) {
    var tenant = tenants.require(org, false);
    var criteria = FilterParser.parse(filters, LessonSpecifications.FILTERS.keySet());
    var spec =
        LessonSpecifications.tenant(tenant)
            .and(SpecificationBuilder.build(criteria, LessonSpecifications.FILTERS));
    return PageResponse.from(
        core.paginate(spec, page.pageable(LessonSpecifications.SORTS)).map(LessonResponse::from));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public LessonResponse create(UUID org, CreateLessonRequest request) {
    var tenant = tenants.require(org, true);
    return LessonResponse.from(
        core.create(new Lesson(tenant.organizationId(), request.title(), request.description())));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public LessonResponse update(UUID org, UUID id, UpdateLessonRequest request) {
    return LessonResponse.from(
        core.update(
            scope(org, id, true),
            e -> {
              check(e, request.version());
              e.edit(request.title(), request.description());
            }));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public LessonResponse publish(UUID org, UUID id, Long version) {
    return LessonResponse.from(
        core.update(
            scope(org, id, true),
            e -> {
              check(e, version);
              e.publish();
            }));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public void delete(UUID org, UUID id, Long version) {
    var scoped = scope(org, id, true);
    check(core.findOneOrFailByCriteria(scoped), version);
    core.delete(scoped);
  }

  private void check(Lesson lesson, Long version) {
    if (!Objects.equals(lesson.getVersion(), version)) throw new ConflictException("Stale version");
  }
}
