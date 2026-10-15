// Device certification example: check whether the app runs on certified
// hardware, with isDeviceCertified and getSerialNumber (added in API 26.1).
//
// Expected output on a certified device:
//
//     IsDeviceCertified: true
//     SerialNumber:      <certification serial>
//
// Expected output on a community board (for example a Raspberry Pi):
//
//     IsDeviceCertified: false
//     SerialNumber:      (empty)
package org.orbitos.examples.devicecertification;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.SystemManager;

public class App {

    static final String TAG = "device_certification";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Device Certification";

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0].trim() : DEFAULT_HOST;

        Logger.Init(APP_NAME, "INFO", true);
        Logger.Infof(TAG, "Starting %s", APP_NAME);

        Client client;
        try {
            client = Client.connect(host, TAG);
        } catch (Exception e) {
            Logger.Fatalf(TAG, "Connect: %s", e.getMessage());
            System.exit(1);
            return;
        }

        int exitCode = 0;
        try {
            run(client);
        } catch (Exception e) {
            Logger.Fatalf(TAG, "%s", e.getMessage());
            exitCode = 1;
        } finally {
            client.close();
        }
        if (exitCode == 0) {
            Logger.Info(TAG, "Done.");
        }
        System.exit(exitCode);
    }

    static void run(Client client) throws Exception {
        SystemManager sys = client.systemManager();

        // These calls need a device with API 26.1 or newer.
        SystemManager.ApiVersion api = sys.getApiVersion();
        Logger.Infof(TAG, "Device API version: %d.%d", api.version, api.revision);

        Logger.Info(TAG, "--- Device certification");

        boolean certified = sys.isDeviceCertified();
        Logger.Infof(TAG, "IsDeviceCertified: %b", certified);

        // The certification serial is empty on a device that is not certified.
        String serial = sys.getSerialNumber();
        Logger.Infof(TAG, "SerialNumber:      %s", serial.isEmpty() ? "(empty)" : serial);

        // Board information, for comparison. BoardSerial is the serial of the
        // hardware itself; it is not the certification serial.
        Logger.Info(TAG, "--- Board information");
        Logger.Infof(TAG, "BoardVendor:       %s", sys.getBoardVendor());
        Logger.Infof(TAG, "BoardModel:        %s", sys.getBoardModel());
        Logger.Infof(TAG, "BoardSerial:       %s", sys.getBoardSerial());

        if (certified) {
            Logger.Infof(TAG, "This is a certified device (serial %s).", serial);
        } else {
            Logger.Info(TAG, "This is a community board: it has no certification serial.");
        }
    }
}
