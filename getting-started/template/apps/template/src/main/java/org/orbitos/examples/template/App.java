// Template: the smallest useful Orbit OS app. It connects to the device and
// reads one value.
//
// To start your own app: copy this project folder, rename it, apps/template and
// the package, update orbit.project.json, settings.gradle, apps/template/build.gradle,
// proguard-rules.pro and metadata.json, then replace the body of run with your own code.
package org.orbitos.examples.template;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;

public class App {

    static final String TAG = "template";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "App template";

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

    // run holds the app logic. Throw an exception to stop the app with exit code 1.
    static void run(Client client) throws Exception {
        String model = client.systemManager().getHardwareModel();
        Logger.Infof(TAG, "Hardware model: %s", model);

        // Add your code here. To call another service, add it to "permissions"
        // in metadata.json first: Orbit OS refuses calls to services that are
        // not listed there.
    }
}
