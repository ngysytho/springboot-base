package vn.mpl.one.modules.lesson.infrastructure;

import java.util.UUID;
import vn.mpl.one.core.repository.BaseRepository;
import vn.mpl.one.modules.lesson.domain.Lesson;

public interface LessonRepository extends BaseRepository<Lesson, UUID> {}
