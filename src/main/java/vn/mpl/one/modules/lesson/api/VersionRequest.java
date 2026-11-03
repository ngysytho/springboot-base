package vn.mpl.one.modules.lesson.api;

import jakarta.validation.constraints.*;

public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
