package vn.mpl.one.modules.lesson.application.usecase;

import java.util.UUID;
import vn.mpl.one.modules.lesson.domain.model.Lesson;

public interface CreateLessonUseCase {
  Lesson create(UUID organizationId, CreateLessonCommand command);
}
