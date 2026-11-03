package vn.mpl.one.modules.lesson.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.infrastructure.persistence.entity.LessonJpaEntity;

@Component
public class LessonPersistenceMapper {
  public Lesson toDomain(LessonJpaEntity entity) {
    return new Lesson(entity.getId(), entity.getOrganizationId(), entity.getTitle(), entity.getDescription(),
        entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
  }
  public LessonJpaEntity toNewEntity(Lesson lesson) {
    return new LessonJpaEntity(lesson.organizationId(), lesson.title(), lesson.description(), lesson.status());
  }
  public void copyContent(Lesson lesson, LessonJpaEntity entity) {
    entity.updateContent(lesson.title(), lesson.description(), lesson.status());
  }
}
