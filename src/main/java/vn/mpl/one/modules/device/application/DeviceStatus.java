package vn.mpl.one.modules.device.application;

/** Extension boundary. No implementation is registered in the foundation. */
public interface DeviceStatus {
  enum State {
    OFFLINE,
    ONLINE,
    REVOKED
  }

  State status(java.util.UUID deviceId);
}
