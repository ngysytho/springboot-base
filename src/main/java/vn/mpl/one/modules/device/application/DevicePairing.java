package vn.mpl.one.modules.device.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface DevicePairing {
  String beginPairing(java.util.UUID deviceId);

  void approvePairing(String code, java.util.UUID organizationId);
}
