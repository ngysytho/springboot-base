package vn.mpl.one.modules.device.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface DeviceAuthorization {
  boolean authorizeDevice(String credential, java.util.UUID organizationId);
}
