package vn.mpl.one.modules.lesson.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.mpl.one.core.exception.ResourceNotFoundException;
import vn.mpl.one.core.filter.FilterCriteria;
import vn.mpl.one.core.pagination.*;
import vn.mpl.one.core.security.TenantAuthorizationService;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.domain.repository.LessonRepository;
import vn.mpl.one.modules.lesson.application.usecase.*;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN','TEACHER','STUDENT','PARENT','BUSINESS_MANAGER')")
public class LessonService implements CreateLessonUseCase {
  private final LessonRepository lessons;
  private final TenantAuthorizationService tenants;

  public LessonService(LessonRepository lessons, TenantAuthorizationService tenants) {
    this.lessons = lessons; this.tenants = tenants;
  }
  public Lesson get(UUID org, UUID id) {
    tenants.require(org, false);
    return load(org, id);
  }
  public PageResponse<Lesson> list(UUID org, PaginationRequest page, FilterCriteria filters) {
    tenants.require(org, false);
    return lessons.findByCriteria(org, filters, page);
  }
  @Override @Transactional @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public Lesson create(UUID org, CreateLessonCommand command) {
    var tenant = tenants.require(org, true);
    return lessons.create(Lesson.draft(tenant.organizationId(), command.title(), command.description()));
  }
  @Transactional @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public Lesson update(UUID org, UUID id, UpdateLessonCommand command) {
    tenants.require(org, true);
    Lesson lesson = load(org, id);
    lesson.requireVersion(command.version());
    return lessons.save(lesson.edit(command.title(), command.description()));
  }
  @Transactional @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public Lesson publish(UUID org, UUID id, Long version) {
    tenants.require(org, true);
    Lesson lesson = load(org, id);
    lesson.requireVersion(version);
    return lessons.save(lesson.publish());
  }
  @Transactional @PreAuthorize("hasAnyRole('ORG_ADMIN','TEACHER')")
  public void delete(UUID org, UUID id, Long version) {
    tenants.require(org, true);
    load(org, id).requireVersion(version);
    lessons.delete(org, id, version);
  }
  private Lesson load(UUID org, UUID id) {
    return lessons.findById(org, id).orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
  }
}
