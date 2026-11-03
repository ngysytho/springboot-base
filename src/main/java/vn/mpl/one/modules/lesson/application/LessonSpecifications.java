package vn.mpl.one.modules.lesson.application;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import vn.mpl.one.core.security.TenantContext;
import vn.mpl.one.modules.lesson.domain.*;

public final class LessonSpecifications {
  private LessonSpecifications() {}

  public static final Map<String, Class<?>> FILTERS =
      Map.of(
          "title",
          String.class,
          "description",
          String.class,
          "status",
          LessonStatus.class,
          "createdAt",
          Instant.class,
          "updatedAt",
          Instant.class,
          "version",
          Long.class);
  public static final Set<String> SORTS = Set.of("title", "status", "createdAt", "updatedAt", "id");

  public static Specification<Lesson> tenant(TenantContext tenant) {
    return (r, q, b) -> b.equal(r.get("organizationId"), tenant.organizationId());
  }

  public static Specification<Lesson> id(UUID id) {
    return (r, q, b) -> b.equal(r.get("id"), id);
  }
}
