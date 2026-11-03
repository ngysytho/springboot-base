package vn.mpl.one.modules.ai.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface AiGenerationService {
  String generate(java.util.UUID organizationId, String prompt);
}
