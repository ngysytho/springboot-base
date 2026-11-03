package vn.mpl.one.modules.lesson.infrastructure.persistence.repository;

import java.util.UUID;
import vn.mpl.one.core.repository.BaseRepository;
import vn.mpl.one.modules.lesson.infrastructure.persistence.entity.LessonJpaEntity;

public interface LessonJpaRepository extends BaseRepository<LessonJpaEntity, UUID> {}
