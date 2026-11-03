package vn.mpl.one.modules.workflow.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface WorkflowState {
  enum Product {
    CENTRE_CARE,
    SHIFT_HUB,
    FAMILY_CIRCLE
  }

  void transition(
      java.util.UUID organizationId,
      Product product,
      java.util.UUID workflowId,
      String expectedState,
      String nextState);
}
