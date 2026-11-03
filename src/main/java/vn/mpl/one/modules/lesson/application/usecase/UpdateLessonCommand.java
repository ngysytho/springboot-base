package vn.mpl.one.modules.lesson.application.usecase;

public record UpdateLessonCommand(String title, String description, Long version) {}
