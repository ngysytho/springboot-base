package vn.mpl.one.modules.classroom.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface InteractiveSession {
  java.util.UUID start(java.util.UUID organizationId, java.util.UUID lessonId);

  void end(java.util.UUID sessionId);
}
