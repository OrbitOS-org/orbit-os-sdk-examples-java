// SPI example: list the SPI devices, configure one, read its configuration
// and run a full-duplex transfer.
//
// Set the constants below to match your wiring.
package org.orbitos.examples.spi;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.SpiManager;

public class App {

    static final String TAG = "spi";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "SPI example";

    // SPI_BUS and SPI_CS select the device: bus 0, chip select 2 is "spidev0.2".
    static final int SPI_BUS = 0;
    static final int SPI_CS = 2;
    // SPI_SPEED_HZ is the maximum clock speed.
    static final int SPI_SPEED_HZ = 10_000;
    // SPI_BITS_PER_WORD, SPI_MODE and SPI_LSB_FIRST set the frame format.
    static final int SPI_BITS_PER_WORD = 8;
    static final SpiManager.SpiMode SPI_MODE = SpiManager.SpiMode.MODE_0;
    static final boolean SPI_LSB_FIRST = false;
    // SPI_LOOPBACK: with MOSI wired to MISO, set this to true to send a short
    // pattern and check that the same bytes come back.
    static final boolean SPI_LOOPBACK = false;

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
        SpiManager spi = client.spiManager();

        List<String> devices = spi.listDevices();
        Logger.Infof(TAG, "SPI devices: %s", devices);

        spi.setConfig(new SpiManager.SpiConfig(SPI_BUS, SPI_CS, SPI_SPEED_HZ, SPI_BITS_PER_WORD, SPI_LSB_FIRST, SPI_MODE));

        SpiManager.SpiConfig cfg = spi.getConfig(SPI_BUS, SPI_CS);
        Logger.Infof(TAG, "Config spidev%d.%d: speed=%d bits=%d mode=%s lsbFirst=%b",
                SPI_BUS, SPI_CS, cfg.maxSpeedHz(), cfg.bitsPerWord(), cfg.mode(), cfg.lsbFirst());

        // SPI is full duplex: for every byte sent, one byte is received.
        byte[] tx = SPI_LOOPBACK
                ? new byte[] {(byte) 0xAB, (byte) 0xCD, 0x12, 0x34}
                : new byte[] {(byte) 0xD1, 0x00, 0x00, 0x01, (byte) 0xFF, (byte) 0xFF};

        byte[] rx = spi.transfer(SPI_BUS, SPI_CS, tx, tx.length);
        HexFormat hex = HexFormat.of();
        Logger.Infof(TAG, "TX %s", hex.formatHex(tx));
        Logger.Infof(TAG, "RX %s", hex.formatHex(rx));

        if (SPI_LOOPBACK && !Arrays.equals(tx, rx)) {
            throw new Exception("loopback failed: sent " + hex.formatHex(tx) + ", received " + hex.formatHex(rx));
        }
    }
}
