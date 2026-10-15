// Notification options example: send a push notification with the optional
// fields of MobileNotificationManager.send (added in API 26.1).
//
// See the mobile_notification example first: it sends the simplest
// notification and explains what the result means.
package org.orbitos.examples.notificationoptions;

import java.util.Map;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.MobileNotificationManager;

public class App {

    static final String TAG = "notification_options";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Mobile Notification options";

    // NOTIFICATION_BODY is the text shown in the notification.
    static final String NOTIFICATION_BODY = "Water tank level is low";
    // COLLAPSE_KEY groups notifications: a new one replaces an earlier one with
    // the same key that the phone has not shown yet.
    static final String COLLAPSE_KEY = "tank-level";
    // TTL_SECONDS is how long the notification is kept for a phone that is
    // offline. After that it is dropped.
    static final int TTL_SECONDS = 3600;

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
        MobileNotificationManager.SendOptions options = new MobileNotificationManager.SendOptions(
                // imageUrl: no image.
                null,
                // data is delivered to the mobile app with the notification, for
                // example to open a specific screen. It is not shown to the user.
                Map.of(
                        "screen", "tank",
                        "level", "12"),
                COLLAPSE_KEY,
                TTL_SECONDS);

        MobileNotificationManager.SendResult result = client.mobileNotificationManager().send(NOTIFICATION_BODY, options);

        // sent and failed count the phones the notification was sent to.
        Logger.Infof(TAG, "Notification sent: sent=%d failed=%d", result.sent(), result.failed());
    }
}
