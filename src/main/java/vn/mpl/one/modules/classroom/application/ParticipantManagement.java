package vn.mpl.one.modules.classroom.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface ParticipantManagement {
  void join(java.util.UUID sessionId, String subject);

  void leave(java.util.UUID sessionId, String subject);
}
