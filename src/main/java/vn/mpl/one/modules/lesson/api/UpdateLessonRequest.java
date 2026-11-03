package vn.mpl.one.modules.lesson.api;

import jakarta.validation.constraints.*;

public record UpdateLessonRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String description,
    @NotNull @PositiveOrZero Long version) {}
