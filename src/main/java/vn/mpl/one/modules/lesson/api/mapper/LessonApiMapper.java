package vn.mpl.one.modules.lesson.api.mapper;

import org.springframework.stereotype.Component;
import vn.mpl.one.modules.lesson.domain.model.Lesson;
import vn.mpl.one.modules.lesson.api.dto.request.*;
import vn.mpl.one.modules.lesson.api.dto.response.LessonResponse;
import vn.mpl.one.modules.lesson.application.usecase.*;

@Component
public class LessonApiMapper {
  public CreateLessonCommand toCommand(CreateLessonRequest request) {
    return new CreateLessonCommand(request.title(), request.description());
  }
  public UpdateLessonCommand toCommand(UpdateLessonRequest request) {
    return new UpdateLessonCommand(request.title(), request.description(), request.version());
  }
  public LessonResponse toResponse(Lesson lesson) {
    return new LessonResponse(lesson.id(), lesson.organizationId(), lesson.title(), lesson.description(),
        lesson.status(), lesson.createdAt(), lesson.updatedAt(), lesson.version());
  }
}
