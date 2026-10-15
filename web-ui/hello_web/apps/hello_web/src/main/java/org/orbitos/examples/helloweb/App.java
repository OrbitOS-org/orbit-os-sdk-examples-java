// Hello web example: an app with a web page, opened from the Orbit OS Launcher.
//
// The app listens on 127.0.0.1 only and registers the page with the Launcher.
// The Launcher serves it at http://<device>/hello_web, asks for the device
// login and forwards the requests to the app. Nothing is reachable from the
// network without going through the Launcher.
package org.orbitos.examples.helloweb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.AppHubManager;

public class App {

    static final String TAG = "hello_web";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Hello web";

    // ROUTE is the path the Launcher serves the page at: http://<device>/hello_web.
    static final String ROUTE = "/hello_web";
    // Web interface ports are reserved to the range PORT_MIN–PORT_MAX. The app
    // starts at PREFERRED_PORT and moves to the next one when a port is taken.
    static final int PORT_MIN = 50000;
    static final int PORT_MAX = 60000;
    static final int PREFERRED_PORT = 50080;

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
        // Read the values shown on the page once, at start-up.
        String deviceName = client.systemManager().getDeviceName();
        String hardwareModel = client.systemManager().getHardwareModel();

        byte[] page;
        try (InputStream in = App.class.getResourceAsStream("/page.html")) {
            if (in == null) {
                throw new Exception("page.html is missing from the app");
            }
            page = in.readAllBytes();
        }

        HttpServer server = listenAndRegister(client.appHubManager());
        server.createContext("/", exchange -> send(exchange, "text/html; charset=utf-8", page));
        server.createContext("/api/info", exchange -> {
            String time = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String json = String.format("{\"deviceName\":%s,\"hardwareModel\":%s,\"time\":%s}",
                    jsonString(deviceName), jsonString(hardwareModel), jsonString(time));
            send(exchange, "application/json", json.getBytes(StandardCharsets.UTF_8));
        });
        server.start();
        Logger.Infof(TAG, "Listening on %s, registered at %s", server.getAddress(), ROUTE);

        try {
            stopRequested.await();
        } finally {
            // Give the requests in progress a moment to finish.
            server.stop(5);
            // Remove the page from the Launcher when the app stops.
            try {
                client.appHubManager().unregisterService();
            } catch (AppHubManager.AppHubException e) {
                Logger.Errorf(TAG, "UnregisterService: %s", e.getMessage());
            }
        }
    }

    // listenAndRegister finds a free port in the reserved range, starting at
    // PREFERRED_PORT, and registers the page with the Launcher. It returns the
    // server, bound and not started yet.
    static HttpServer listenAndRegister(AppHubManager appHub) throws Exception {
        int span = PORT_MAX - PORT_MIN + 1;
        for (int i = 0; i < span; i++) {
            int port = PORT_MIN + (PREFERRED_PORT - PORT_MIN + i) % span;

            HttpServer server;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            } catch (BindException e) {
                continue; // port taken: try the next one
            }
            try {
                appHub.registerWebUI("127.0.0.1", port, ROUTE);
            } catch (AppHubManager.AppHubException e) {
                server.stop(0); // the Launcher refused this port: try the next one
                Logger.Warnf(TAG, "RegisterWebUI 127.0.0.1:%d: %s", port, e.getMessage());
                continue;
            }
            return server;
        }
        throw new Exception("no free web interface port in " + PORT_MIN + "-" + PORT_MAX);
    }

    static void send(HttpExchange exchange, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    // jsonString quotes a value for JSON.
    static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
