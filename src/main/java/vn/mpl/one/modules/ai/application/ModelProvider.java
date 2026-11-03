package vn.mpl.one.modules.ai.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface ModelProvider {
  String providerId();

  String generate(String model, String prompt);
}
