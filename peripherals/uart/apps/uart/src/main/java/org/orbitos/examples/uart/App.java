// UART example: open a serial port at 9600 8N1 and print what it receives,
// until Ctrl+C.
//
// The received bytes go to stdout as they arrive; the log lines go through the
// SDK logger. In a terminal the two may interleave.
//
// Set the constants below to match your device.
package org.orbitos.examples.uart;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.UartManager;

public class App {

    static final String TAG = "uart";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "UART example";

    // UART_PORT_NAME must match a name returned by listPorts.
    static final String UART_PORT_NAME = "ttyUSB0";
    // UART_BAUD_RATE is the line speed.
    static final int UART_BAUD_RATE = 9600;
    // RX_CHUNK_SIZE is the most bytes handed to the receive callback at once.
    static final int RX_CHUNK_SIZE = 256;

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

    static void run(Client client) throws Exception {
        UartManager uart = client.uartManager();

        List<String> ports = uart.listPorts();
        Logger.Infof(TAG, "UART ports: %s", ports);
        if (!ports.contains(UART_PORT_NAME)) {
            throw new Exception("port " + UART_PORT_NAME + " not found on this device (available: " + ports + ")");
        }

        Logger.Infof(TAG, "Opening %s at %d 8N1 (Ctrl+C to stop)", UART_PORT_NAME, UART_BAUD_RATE);
        uart.open(new UartManager.UartConfig(UART_PORT_NAME, UART_BAUD_RATE, 8,
                UartManager.Parity.NONE, UartManager.StopBits.ONE, UartManager.FlowControl.NONE));

        // Release the port when done: only one app can hold a UART port at a time.
        try {
            // Read the configuration back to confirm what the device applied.
            UartManager.UartConfig cfg = uart.getConfig(UART_PORT_NAME);
            Logger.Infof(TAG, "Active config: port=%s baud=%d dataBits=%d parity=%s stopBits=%s flow=%s",
                    cfg.port(), cfg.baudRate(), cfg.dataBits(), cfg.parity(), cfg.stopBits(), cfg.flowControl());

            // listenAsync calls onReceive for each chunk, on another thread, until
            // it is cancelled. An error ends the stream and stops the app.
            AtomicReference<Throwable> streamError = new AtomicReference<>();
            UartManager.StreamHandle handle = uart.listenAsync(UART_PORT_NAME, RX_CHUNK_SIZE, App::onReceive,
                    error -> {
                        streamError.compareAndSet(null, error);
                        stopRequested.countDown();
                    });

            stopRequested.await();
            Throwable error = streamError.get();
            handle.cancel();
            if (error != null) {
                throw new Exception("listen: " + error.getMessage());
            }
        } finally {
            try {
                uart.close(UART_PORT_NAME);
            } catch (UartManager.UartException e) {
                Logger.Errorf(TAG, "Close: %s", e.getMessage());
            }
        }
    }

    // onReceive writes the received bytes to stdout as they are.
    static void onReceive(byte[] chunk) {
        System.out.write(chunk, 0, chunk.length);
        System.out.flush();
    }
}
