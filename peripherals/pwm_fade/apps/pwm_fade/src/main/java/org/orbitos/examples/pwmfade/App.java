// PWM fade example: fade the first PWM channel in and out, like a breathing
// LED, until Ctrl+C. The output is stopped when the example ends.
package org.orbitos.examples.pwmfade;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.PwmManager;

public class App {

    static final String TAG = "pwm_fade";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "PWM fade";

    // FREQUENCY_HZ is the output frequency. 1 kHz is fast enough for an LED
    // not to flicker.
    static final double FREQUENCY_HZ = 1000.0;
    // STEPS is the number of duty cycle levels between off and fully on.
    static final int STEPS = 50;
    // STEP_INTERVAL_MS is the time spent on each level.
    static final long STEP_INTERVAL_MS = 20;

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
        PwmManager pwm = client.pwmManager();

        List<PwmManager.PwmChannel> channels = pwm.listChannels();
        if (channels.isEmpty()) {
            Logger.Info(TAG, "No PWM channel on this device: nothing to do.");
            return;
        }

        PwmManager.PwmChannel channel = channels.get(0);
        Logger.Infof(TAG, "Fading \"%s\" at %s Hz. Ctrl+C to stop.", channel.name(), FREQUENCY_HZ);

        try {
            // step goes 0, 1, … STEPS, then back down to 0, and repeats.
            int step = 0;
            int direction = 1;
            do {
                pwm.setPwm(channel, (double) step / STEPS, FREQUENCY_HZ);

                if (step == STEPS) {
                    direction = -1;
                } else if (step == 0) {
                    direction = 1;
                }
                step += direction;
            } while (sleep(STEP_INTERVAL_MS));
        } finally {
            // Stop the output when done, so the channel is not left running.
            try {
                pwm.stopPwm(channel);
            } catch (PwmManager.PwmException e) {
                Logger.Errorf(TAG, "StopPwm: %s", e.getMessage());
            }
        }
    }
}
