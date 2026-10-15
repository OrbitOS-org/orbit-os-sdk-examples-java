// Ethernet status example: list the Ethernet interfaces and print the link
// state and IP configuration of each one.
package org.orbitos.examples.ethstatus;

import java.util.List;

import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.EthernetManager;

public class App {

    static final String TAG = "eth_status";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Ethernet status example";

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
        EthernetManager eth = client.ethernetManager();

        List<EthernetManager.EthernetInterfaceInfo> ifaces = eth.listInterfaces();
        Logger.Infof(TAG, "Ethernet interfaces: %d", ifaces.size());
        for (int i = 0; i < ifaces.size(); i++) {
            EthernetManager.EthernetInterfaceInfo iface = ifaces.get(i);
            String name = iface.interfaceName();

            // A failure on one interface should not hide the others.
            boolean linkUp;
            try {
                linkUp = eth.isConnected(name);
            } catch (EthernetManager.EthernetException e) {
                Logger.Errorf(TAG, "  [%d] isConnected(\"%s\"): %s", i, name, e.getMessage());
                continue;
            }

            Logger.Infof(TAG, "  [%d] \"%s\" linkUp=%b state=%s mac=\"%s\" mtu=%d dhcp=%b ipv4=\"%s\" gw=\"%s\" dns=[%s]",
                    i, name, linkUp, iface.state(), iface.macAddress(), iface.mtu(), iface.dhcpEnabled(),
                    iface.ipv4Address(), iface.ipv4Gateway(), String.join(",", iface.ipv4Dns()));
        }
    }
}
