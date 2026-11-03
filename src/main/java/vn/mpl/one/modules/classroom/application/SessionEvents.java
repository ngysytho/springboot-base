package vn.mpl.one.modules.classroom.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface SessionEvents {
  void emit(java.util.UUID sessionId, String eventType, java.util.Map<String, Object> payload);
}
