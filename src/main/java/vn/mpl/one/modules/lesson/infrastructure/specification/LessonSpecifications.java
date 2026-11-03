package vn.mpl.one.modules.lesson.infrastructure.specification;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.filter.*;
import vn.mpl.one.modules.lesson.domain.enums.LessonStatus;
import vn.mpl.one.modules.lesson.infrastructure.persistence.entity.LessonJpaEntity;
import static vn.mpl.one.core.filter.FilterOperator.*;

public final class LessonSpecifications {
  private LessonSpecifications() {}
  private static final Set<FilterOperator> TEXT = Set.of(EQ, NEQ, LIKE, IN, IS_NULL, IS_NOT_NULL);
  private static final Set<FilterOperator> ORDERED = Set.of(EQ, NEQ, GT, GTE, LT, LTE, IN, IS_NULL, IS_NOT_NULL);
  public static final Map<String, FilterField> FILTERS = Map.of(
      "title", new FilterField(String.class, TEXT),
      "description", new FilterField(String.class, TEXT),
      "status", new FilterField(LessonStatus.class, Set.of(EQ, NEQ, IN, IS_NULL, IS_NOT_NULL)),
      "createdAt", new FilterField(Instant.class, ORDERED),
      "updatedAt", new FilterField(Instant.class, ORDERED),
      "version", new FilterField(Long.class, ORDERED));
  public static final Set<String> SORTS = Set.of("title", "status", "createdAt", "updatedAt", "id");

  public static Specification<LessonJpaEntity> tenant(UUID organizationId) {
    Objects.requireNonNull(organizationId, "Organization scope is required");
    return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
  }
  public static Specification<LessonJpaEntity> id(UUID id) {
    Objects.requireNonNull(id);
    return (root, query, cb) -> cb.equal(root.get("id"), id);
  }
}
