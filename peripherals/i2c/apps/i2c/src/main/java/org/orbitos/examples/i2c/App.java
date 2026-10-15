// I2C example: list the I2C buses, configure the first one, read its
// configuration and scan it for devices.
package org.orbitos.examples.i2c;

import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.I2CManager;

public class App {

    static final String TAG = "i2c";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "I2C example";

    // CLOCK_HZ is the bus speed: 100000 is standard mode (100 kHz).
    static final int CLOCK_HZ = 100000;
    // TEN_BIT_ADDR selects 10-bit addressing. Most devices use 7-bit addresses.
    static final boolean TEN_BIT_ADDR = false;
    // CLOCK_STRETCHING lets a slow device hold the clock line while it gets ready.
    static final boolean CLOCK_STRETCHING = true;

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
        I2CManager i2c = client.i2cManager();

        List<Integer> buses = i2c.listBuses();
        Logger.Infof(TAG, "I2C buses: %s", buses);
        if (buses.isEmpty()) {
            Logger.Info(TAG, "No I2C bus on this device: nothing to do.");
            return;
        }

        int bus = buses.get(0);
        Logger.Infof(TAG, "Using bus %d", bus);

        i2c.setConfig(new I2CManager.I2CConfig(bus, CLOCK_HZ, TEN_BIT_ADDR, CLOCK_STRETCHING));

        I2CManager.I2CConfig cfg = i2c.getConfig(bus);
        Logger.Infof(TAG, "Config: clockHz=%d tenBit=%b clockStretching=%b",
                cfg.clockHz(), cfg.tenBitAddr(), cfg.clockStretching());

        List<Integer> addrs = i2c.scanBus(bus);
        if (addrs.isEmpty()) {
            Logger.Infof(TAG, "No device answered on bus %d.", bus);
            return;
        }
        Logger.Infof(TAG, "%d device(s) on bus %d:", addrs.size(), bus);
        for (int addr : addrs) {
            Logger.Infof(TAG, "  address 0x%02X", addr);
        }

        // To read from a device, use one of the addresses found above. For example,
        // to read 1 byte from the device at 0x48:
        //
        //     byte[] data = i2c.transfer(new I2CManager.I2CTransferRequest(bus, 0x48, new byte[0], 1));
    }
}
