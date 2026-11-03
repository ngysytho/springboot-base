package vn.mpl.one.modules.lesson.api;

import jakarta.validation.constraints.*;

public record CreateLessonRequest(
    @NotBlank @Size(max = 200) String title, @Size(max = 10000) String description) {}
