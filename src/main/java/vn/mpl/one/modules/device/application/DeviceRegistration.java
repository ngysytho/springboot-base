package vn.mpl.one.modules.device.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface DeviceRegistration {
  java.util.UUID register(java.util.UUID organizationId, String hardwareReference);
}
