// WiFi scan example: list the WiFi interfaces, then scan for networks on the
// first one.
package org.orbitos.examples.wifiscan;

import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.WiFiManager;

public class App {

    static final String TAG = "wifi_scan";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "WiFi scan example";

    // FORCE_RESCAN asks the hardware for a fresh scan, which takes about 3 seconds.
    // Set it to false to get the cached results at once.
    static final boolean FORCE_RESCAN = true;

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
        Logger.Infof(TAG, "WiFi interfaces: %d", ifaces.size());
        for (int i = 0; i < ifaces.size(); i++) {
            WiFiManager.WiFiInterfaceInfo iface = ifaces.get(i);
            Logger.Infof(TAG, "  [%d] name=\"%s\" mac=\"%s\" mode=%s state=%s",
                    i, iface.interfaceName(), iface.macAddress(), iface.mode(), iface.state());
        }
        if (ifaces.isEmpty()) {
            Logger.Info(TAG, "No WiFi interface on this device: nothing to scan.");
            return;
        }

        String ifname = ifaces.get(0).interfaceName();
        Logger.Infof(TAG, "Scanning on \"%s\" (forceRescan=%b)", ifname, FORCE_RESCAN);

        List<WiFiManager.ScannedNetwork> networks = wifi.scan(ifname, FORCE_RESCAN);
        Logger.Infof(TAG, "Networks found: %d", networks.size());
        for (int i = 0; i < networks.size(); i++) {
            WiFiManager.ScannedNetwork n = networks.get(i);
            Logger.Infof(TAG, "  [%d] ssid=\"%s\" bssid=\"%s\" security=\"%s\" rssi=%d ch=%d band=\"%s\" hidden=%b",
                    i, n.ssid(), n.bssid(), n.security(), n.signalStrength(), n.channel(), n.band(), n.hidden());
        }
    }
}
