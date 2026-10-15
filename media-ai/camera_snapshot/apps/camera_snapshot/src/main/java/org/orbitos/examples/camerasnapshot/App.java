// Camera snapshot example: list the cameras, lock the first one, take one
// picture, save it to a file and release the camera.
//
// On the device the file is written to the app's data folder, which is the
// working directory of the app.
package org.orbitos.examples.camerasnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.CameraManager;

public class App {

    static final String TAG = "camera_snapshot";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Camera snapshot";

    // CAPTURE_WIDTH, CAPTURE_HEIGHT and CAPTURE_FORMAT describe the picture to
    // take. The camera must support them: see the formats and resolutions the
    // example prints for each camera.
    static final int CAPTURE_WIDTH = 640;
    static final int CAPTURE_HEIGHT = 480;
    static final String CAPTURE_FORMAT = "mjpeg";
    // OUTPUT_FILE is where the picture is saved. An MJPEG frame is a JPEG image.
    static final String OUTPUT_FILE = "snapshot.jpg";

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
        CameraManager camera = client.cameraManager();

        List<CameraManager.DeviceInfo> devices = camera.listDevices();
        Logger.Infof(TAG, "Cameras: %d", devices.size());
        for (int i = 0; i < devices.size(); i++) {
            CameraManager.DeviceInfo d = devices.get(i);
            Logger.Infof(TAG, "  [%d] id=\"%s\" card=\"%s\" driver=\"%s\" formats=%s resolutions=%s",
                    i, d.deviceId(), d.card(), d.driver(), d.supportedFormats(), d.resolutions());
        }
        if (devices.isEmpty()) {
            Logger.Info(TAG, "No camera on this device: nothing to capture.");
            return;
        }

        // Only one app can hold a camera at a time. Lock it, use it, unlock it:
        // try-with-resources unlocks it when the block ends.
        String deviceId = devices.get(0).deviceId();
        try (CameraManager.LockedCamera locked = camera.lock(deviceId, TAG)) {
            Logger.Infof(TAG, "Capturing %dx%d %s from %s", CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FORMAT, deviceId);
            CameraManager.CaptureResult image = locked.captureImage(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FORMAT);
            if (image.imageData().length == 0) {
                throw new Exception("the camera returned an empty image");
            }

            Files.write(Path.of(OUTPUT_FILE), image.imageData());
            Logger.Infof(TAG, "Saved %s (%d bytes, format %s)", OUTPUT_FILE, image.imageData().length, image.format());
        }
    }
}
