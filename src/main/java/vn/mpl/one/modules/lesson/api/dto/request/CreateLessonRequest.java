package vn.mpl.one.modules.lesson.api.dto.request;

import jakarta.validation.constraints.*;

public record CreateLessonRequest(
    @NotBlank @Size(max = 200) String title, @Size(max = 10000) String description) {}
