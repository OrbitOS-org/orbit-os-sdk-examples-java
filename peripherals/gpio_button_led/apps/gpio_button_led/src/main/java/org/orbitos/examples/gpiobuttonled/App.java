// GPIO button and LED example: the LED is on while the button is pressed.
//
// It reads an input pin and writes its level to an output pin, until Ctrl+C.
// Set the two pin names below to match your wiring.
package org.orbitos.examples.gpiobuttonled;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.GpioManager;

public class App {

    static final String TAG = "gpio_button_led";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "GPIO button and LED";

    // BUTTON_PIN_NAME and LED_PIN_NAME must match names returned by listPins.
    static final String BUTTON_PIN_NAME = "GPIO24";
    static final String LED_PIN_NAME = "GPIO23";
    // POLL_INTERVAL_MS is how often the button is read.
    static final long POLL_INTERVAL_MS = 20;

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
        GpioManager.GpioPin button = findPin(pins, BUTTON_PIN_NAME);
        GpioManager.GpioPin led = findPin(pins, LED_PIN_NAME);

        gpio.setDirection(button, GpioManager.Direction.IN);
        gpio.setDirection(led, GpioManager.Direction.OUT);

        Logger.Infof(TAG, "%s (button) drives %s (LED). Ctrl+C to stop.", BUTTON_PIN_NAME, LED_PIN_NAME);
        try {
            // Write to the LED only when the button changes, not on every read.
            GpioManager.Level last = null;
            do {
                GpioManager.Level level = gpio.getLevel(button);
                if (level != last) {
                    gpio.setLevel(led, level);
                    Logger.Info(TAG, level == GpioManager.Level.HIGH ? "Button pressed: LED on" : "Button released: LED off");
                    last = level;
                }
            } while (sleep(POLL_INTERVAL_MS));
        } finally {
            // Whatever happens, leave the LED off.
            try {
                gpio.setLevel(led, GpioManager.Level.LOW);
            } catch (GpioManager.GpioException e) {
                Logger.Errorf(TAG, "Turn the LED off on exit: %s", e.getMessage());
            }
        }
    }

    // findPin returns the pin with the given name.
    static GpioManager.GpioPin findPin(List<GpioManager.GpioPin> pins, String name) throws Exception {
        return pins.stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new Exception("pin " + name + " not found on this device"));
    }
}
