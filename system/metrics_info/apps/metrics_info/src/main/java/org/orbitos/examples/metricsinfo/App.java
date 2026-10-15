// System metrics example: read one snapshot of the device metrics — CPU,
// memory, disk and temperature — with SystemManager.getMetrics.
package org.orbitos.examples.metricsinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import api.gravity.v26.SystemServiceOuterClass.MetricsInfo;
import api.gravity.v26.SystemServiceOuterClass.MetricsInfoResponse;
import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;

public class App {

    static final String TAG = "metrics_info";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "System metrics example";

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
        MetricsInfoResponse resp = client.systemManager().getMetrics();
        if (!resp.hasMetrics()) {
            Logger.Info(TAG, "The device returned no metrics.");
            return;
        }
        MetricsInfo m = resp.getMetrics();

        // CPU: overall usage, then one value per core.
        List<String> cores = new ArrayList<>();
        for (double v : m.getCpuCorePercentList()) {
            cores.add(String.format("%.2f", v));
        }
        Logger.Infof(TAG, "cpu: usage=%.1f%% cores=[%s]", m.getCpuUsage(), String.join(" ", cores));

        Logger.Infof(TAG, "memory: usage=%.1f%%", m.getMemoryUsage());

        // Disk usage comes as a map of mount point to usage. Sort the mount
        // points so the output is the same on every run.
        for (Map.Entry<String, MetricsInfo.DiskUsage> disk : new TreeMap<>(m.getDiskUsageMap()).entrySet()) {
            Logger.Infof(TAG, "disk %s: usage=%.1f%%", disk.getKey(), disk.getValue().getPercent());
        }

        Logger.Infof(TAG, "thermal: soc=%.1f°C sensors=%d°C", m.getSocThermal(), m.getSensorsTemperatures());
    }
}
