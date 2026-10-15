// PWM example: list the PWM channels, set the first one to a fixed frequency
// and duty cycle, then read the settings back.
//
// The channel keeps running with these settings after the example ends.
package org.orbitos.examples.pwm;

import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.PwmManager;

public class App {

    static final String TAG = "pwm";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "PWM example";

    // DUTY_CYCLE is the fraction of each period the output stays high (0.5 = 50%).
    static final double DUTY_CYCLE = 0.5;
    // FREQUENCY_HZ is the output frequency.
    static final double FREQUENCY_HZ = 10.0;

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
        PwmManager pwm = client.pwmManager();

        List<PwmManager.PwmChannel> channels = pwm.listChannels();
        Logger.Infof(TAG, "PWM channels: %d", channels.size());
        for (int i = 0; i < channels.size(); i++) {
            PwmManager.PwmChannel ch = channels.get(i);
            Logger.Infof(TAG, "  [%d] name=\"%s\" channel=%d", i, ch.name(), ch.channel());
        }
        if (channels.isEmpty()) {
            Logger.Info(TAG, "No PWM channel on this device: nothing to configure.");
            return;
        }

        PwmManager.PwmChannel channel = channels.get(0);
        Logger.Infof(TAG, "Setting \"%s\" to duty=%.0f%% frequency=%s Hz", channel.name(), DUTY_CYCLE * 100, FREQUENCY_HZ);
        pwm.setPwm(channel, DUTY_CYCLE, FREQUENCY_HZ);

        // Read the settings back to confirm what the device applied.
        PwmManager.PwmProperties props = pwm.getProperties(channel);
        Logger.Infof(TAG, "Read back: enabled=%b duty=%.3f frequency=%.3f Hz",
                props.enabled(), props.dutyCycle(), props.frequencyHz());
    }
}
