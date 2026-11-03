package vn.mpl.one.modules.lesson.domain.repository;

import java.util.*;
import vn.mpl.one.core.filter.FilterCriteria;
import vn.mpl.one.core.pagination.*;
import vn.mpl.one.modules.lesson.domain.model.Lesson;

/** Every persisted lookup/mutation is explicitly scoped to an organization. */
public interface LessonRepository {
  Optional<Lesson> findById(UUID organizationId, UUID id);
  PageResponse<Lesson> findByCriteria(UUID organizationId, FilterCriteria filters, PaginationRequest page);
  Lesson create(Lesson lesson);
  Lesson save(Lesson lesson);
  void delete(UUID organizationId, UUID id, Long expectedVersion);
}
