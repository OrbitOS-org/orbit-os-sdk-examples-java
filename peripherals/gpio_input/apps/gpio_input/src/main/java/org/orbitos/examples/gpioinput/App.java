// GPIO input example: wait until an input pin reads high.
//
// Set INPUT_PIN_NAME below to a pin of your board that is configured as an input.
package org.orbitos.examples.gpioinput;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.GpioManager;

public class App {

    static final String TAG = "gpio_input";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "GPIO input example";

    // INPUT_PIN_NAME must match a name returned by listPins.
    static final String INPUT_PIN_NAME = "GPIO24";
    // POLL_INTERVAL_MS is how often the pin level is read.
    static final long POLL_INTERVAL_MS = 50;

    // Ctrl+C, and SIGTERM, which Orbit OS sends to stop an app, start the JVM
    // shutdown: the hook in main asks run to stop, then waits for it to clean up.
    static final CountDownLatch stopRequested = new CountDownLatch(1);
    static final CountDownLatch finished = new CountDownLatch(1);

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopRequested.countDown();
            try {
                finished.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        int exitCode = 0;
        try {
            run(client);
        } catch (Exception e) {
            Logger.Fatalf(TAG, "%s", e.getMessage());
            exitCode = 1;
        } finally {
            client.close();
            finished.countDown();
        }
        if (exitCode == 0) {
            Logger.Info(TAG, "Done.");
        }
        System.exit(exitCode);
    }

    // sleep waits for ms milliseconds. It returns false if the app was asked to stop first.
    static boolean sleep(long ms) throws InterruptedException {
        return !stopRequested.await(ms, TimeUnit.MILLISECONDS);
    }

    static void run(Client client) throws Exception {
        GpioManager gpio = client.gpioManager();

        GpioManager.GpioPin pin = gpio.listPins().stream()
                .filter(p -> p.name().equals(INPUT_PIN_NAME))
                .findFirst()
                .orElseThrow(() -> new Exception("pin " + INPUT_PIN_NAME + " not found on this device"));

        // This example only reads. It does not change the direction of the pin.
        GpioManager.Direction dir = gpio.getDirection(pin);
        if (dir != GpioManager.Direction.IN) {
            throw new Exception("pin " + INPUT_PIN_NAME + " is not an input (direction=" + dir + ")");
        }

        Logger.Infof(TAG, "Waiting for %s to go high (Ctrl+C to stop)", INPUT_PIN_NAME);
        while (gpio.getLevel(pin) != GpioManager.Level.HIGH) {
            if (!sleep(POLL_INTERVAL_MS)) {
                Logger.Infof(TAG, "Stopped before %s went high.", INPUT_PIN_NAME);
                return;
            }
        }
        Logger.Infof(TAG, "%s is high.", INPUT_PIN_NAME);
    }
}
