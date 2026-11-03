package vn.mpl.one.modules.ai.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface AiUsageTracking {
  void record(
      java.util.UUID organizationId,
      String provider,
      String model,
      long inputTokens,
      long outputTokens);
}
