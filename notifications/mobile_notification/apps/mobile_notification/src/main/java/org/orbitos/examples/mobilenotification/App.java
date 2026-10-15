// Mobile notification example: send a push notification to the phones linked
// to the device owner's Orbit OS account, with MobileNotificationManager.send
// (added in API 26.1).
//
// The app only calls send. Orbit OS delivers the notification through the
// Orbit OS Cloud Services; the app holds no cloud credentials.
//
// What you get back depends on the device at the time of the run:
//   - the device is not signed in to an Orbit OS account, or no phone is
//     linked to that account: an error with code ERROR_CODE_UNAVAILABLE;
//   - signed in, with at least one phone linked: sent is 1 or more.
package org.orbitos.examples.mobilenotification;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.MobileNotificationManager;

public class App {

    static final String TAG = "mobile_notification";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Mobile Notification";

    // NOTIFICATION_BODY is the text shown in the notification.
    static final String NOTIFICATION_BODY = "Device status changed";

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
        // There is no title parameter. Orbit OS always sets the title to
        // "<device name> · <this app's name>", taken from the app's identity, so
        // one app cannot send a notification that looks like it came from another.
        //
        // The second argument holds the optional fields. For example, to attach
        // data for the mobile app and replace an earlier notification:
        //
        //     new MobileNotificationManager.SendOptions(null, Map.of("screen", "status"), "status", 0)
        MobileNotificationManager.SendResult result;
        try {
            result = client.mobileNotificationManager().send(NOTIFICATION_BODY, null);
        } catch (MobileNotificationManager.MobileNotificationException e) {
            // getErrorCode is the code returned by Orbit OS, for example
            // ERROR_CODE_UNAVAILABLE.
            throw new Exception(e.getMessage() + " (code: " + e.getErrorCode() + ")");
        }

        // sent and failed count the phones the notification was sent to.
        Logger.Infof(TAG, "Notification sent: sent=%d failed=%d", result.sent(), result.failed());
    }
}
