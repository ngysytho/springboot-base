package vn.mpl.one.modules.lesson.api.dto.request;

import jakarta.validation.constraints.*;

public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
