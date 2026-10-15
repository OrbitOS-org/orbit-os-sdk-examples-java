// WiFi status example: for each WiFi interface, show whether it is connected,
// its mode, and the details of its connection.
//
// The example only reads. It does not change the WiFi configuration.
package org.orbitos.examples.wifistatus;

import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.WiFiManager;

public class App {

    static final String TAG = "wifi_status";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "WiFi status";

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
        WiFiManager wifi = client.wifiManager();

        List<WiFiManager.WiFiInterfaceInfo> ifaces = wifi.listInterfaces();
        if (ifaces.isEmpty()) {
            Logger.Info(TAG, "No WiFi interface on this device.");
            return;
        }

        for (WiFiManager.WiFiInterfaceInfo iface : ifaces) {
            String name = iface.interfaceName();
            Logger.Infof(TAG, "--- %s", name);

            boolean connected = wifi.isConnected(name);
            WiFiManager.WiFiInterfaceInfo link = wifi.getLinkProperties(name);
            Logger.Infof(TAG, "connected=%b mode=%s state=%s mac=\"%s\"", connected, link.mode(), link.state(), link.macAddress());

            // In client mode the link carries the network the interface joined.
            if (link.mode() == WiFiManager.WiFiMode.CLIENT || link.mode() == WiFiManager.WiFiMode.AP_CLIENT) {
                Logger.Infof(TAG, "client: ssid=\"%s\" bssid=\"%s\" rssi=%d ch=%d band=\"%s\"",
                        link.ssid(), link.bssid(), link.signalStrength(), link.channel(), link.band());
                Logger.Infof(TAG, "client: ipv4=\"%s\" gw=\"%s\"", link.ipv4Address(), link.ipv4Gateway());
            }
        }
    }
}
