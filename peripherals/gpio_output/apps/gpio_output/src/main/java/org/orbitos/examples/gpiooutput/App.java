// GPIO output example: list the GPIO lines of the device, then blink the
// first one.
//
// Check which line comes first on your board before you wire anything to it.
package org.orbitos.examples.gpiooutput;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.GpioManager;

public class App {

    static final String TAG = "gpio_output";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "GPIO output example";

    // BLINK_PHASE_MS is how long the line stays high, and then low, in each cycle.
    static final long BLINK_PHASE_MS = 500;
    // BLINK_CYCLES is the number of high-then-low cycles.
    static final int BLINK_CYCLES = 20;

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

        List<GpioManager.GpioPin> pins = gpio.listPins();
        Logger.Infof(TAG, "GPIO lines: %d", pins.size());
        for (int i = 0; i < pins.size(); i++) {
            GpioManager.GpioPin p = pins.get(i);
            Logger.Infof(TAG, "  [%d] name=\"%s\" gpiochip=%d line=%d", i, p.name(), p.gpiochip(), p.line());
        }
        if (pins.isEmpty()) {
            Logger.Info(TAG, "No GPIO line on this device: nothing to blink.");
            return;
        }

        GpioManager.GpioPin pin = pins.get(0);
        Logger.Infof(TAG, "Blinking \"%s\" (gpiochip=%d line=%d)", pin.name(), pin.gpiochip(), pin.line());

        gpio.setDirection(pin, GpioManager.Direction.OUT);
        try {
            for (int cycle = 1; cycle <= BLINK_CYCLES; cycle++) {
                gpio.setLevel(pin, GpioManager.Level.HIGH);
                Logger.Infof(TAG, "  cycle %d/%d high", cycle, BLINK_CYCLES);
                if (!sleep(BLINK_PHASE_MS)) {
                    return;
                }

                gpio.setLevel(pin, GpioManager.Level.LOW);
                Logger.Infof(TAG, "  cycle %d/%d low", cycle, BLINK_CYCLES);
                if (!sleep(BLINK_PHASE_MS)) {
                    return;
                }
            }
        } finally {
            // Whatever happens, leave the line low.
            try {
                gpio.setLevel(pin, GpioManager.Level.LOW);
            } catch (GpioManager.GpioException e) {
                Logger.Errorf(TAG, "Set the line low on exit: %s", e.getMessage());
            }
        }
    }
}
