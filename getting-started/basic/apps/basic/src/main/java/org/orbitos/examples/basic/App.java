// Basic example: connect to the device and read system information —
// API version, device identity, hardware, software versions and live metrics.
//
// Every value comes from SystemManager. The table in run lists each getter by
// name, so the source doubles as a map of what the system service offers.
package org.orbitos.examples.basic;

import java.util.List;

import api.gravity.v26.SystemServiceOuterClass.MetricsInfo;
import api.gravity.v26.SystemServiceOuterClass.MetricsInfoResponse;
import org.orbitos.sdk.Client;
import org.orbitos.sdk.logger.Logger;
import org.orbitos.sdk.services.SystemManager;

public class App {

    static final String TAG = "basic";

    // DEFAULT_HOST is only used in development, when the app runs on your computer
    // and talks to a device in Developer Mode: Orbit Studio passes the device
    // address as the first argument. On the device the SDK connects locally.
    static final String DEFAULT_HOST = "192.168.1.100";

    // APP_NAME is the name in metadata.json, the app manifest that Orbit Studio
    // reads when it builds the .orb package.
    static final String APP_NAME = "Basic example";

    /** One value of the system manager: its name and the call that reads it. */
    interface Getter {
        Object get() throws Exception;
    }

    record Field(String name, Getter getter) {}

    record Section(String title, List<Field> fields) {}

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
        SystemManager sys = client.systemManager();

        // The API version belongs to the device. It is not the version of the SDK
        // this app was built with.
        SystemManager.ApiVersion api = sys.getApiVersion();
        Logger.Infof(TAG, "Device API version: %d.%d", api.version, api.revision);

        List<Section> sections = List.of(
                new Section("Device identity", List.of(
                        new Field("DeviceName", sys::getDeviceName),
                        new Field("SocModel", sys::getSocModel),
                        new Field("SocVendor", sys::getSocVendor),
                        new Field("BoardModel", sys::getBoardModel),
                        new Field("BoardVendor", sys::getBoardVendor),
                        new Field("HardwareModel", sys::getHardwareModel),
                        new Field("HardwareVersion", sys::getHardwareVersion),
                        new Field("SystemUuid", sys::getSystemUuid),
                        new Field("BoardSerial", sys::getBoardSerial),
                        new Field("CpuSerial", sys::getCpuSerial),
                        new Field("MachineId", sys::getMachineId),
                        new Field("Architecture", sys::getArchitecture),
                        new Field("CpuModel", sys::getCpuModel))),
                new Section("Software", List.of(
                        new Field("OsName", sys::getOsName),
                        new Field("OsVersion", sys::getOsVersion),
                        new Field("OsRevision", sys::getOsRevision),
                        new Field("KernelVersion", sys::getKernelVersion),
                        new Field("Distro", sys::getDistro),
                        new Field("DistroVersion", sys::getDistroVersion),
                        new Field("RuntimeVersion", sys::getRuntimeVersion),
                        new Field("RuntimeBuildDate", sys::getRuntimeBuildDate))),
                new Section("CPU and memory", List.of(
                        new Field("TotalRAM", sys::getTotalRAM),
                        new Field("CpuCores", sys::getCpuCores),
                        new Field("CpuThreads", sys::getCpuThreads),
                        new Field("CpuMinMhz", () -> String.format("%.1f", sys.getCpuMinMhz())),
                        new Field("CpuMaxMhz", () -> String.format("%.1f", sys.getCpuMaxMhz())))));

        for (Section section : sections) {
            Logger.Infof(TAG, "--- %s", section.title());
            for (Field field : section.fields()) {
                Logger.Infof(TAG, "%-17s %s", field.name() + ":", field.getter().get());
            }
        }

        // One snapshot of the live metrics. The metrics_info example shows more.
        Logger.Info(TAG, "--- Metrics");

        MetricsInfoResponse resp = sys.getMetrics();
        if (!resp.hasMetrics()) {
            Logger.Info(TAG, "The device returned no metrics.");
            return;
        }
        MetricsInfo m = resp.getMetrics();
        Logger.Infof(TAG, "%-17s %.1f%%", "CpuUsage:", m.getCpuUsage());
        Logger.Infof(TAG, "%-17s %.1f%%", "MemoryUsage:", m.getMemoryUsage());
        Logger.Infof(TAG, "%-17s %.1f°C", "SocThermal:", m.getSocThermal());
        Logger.Infof(TAG, "%-17s %d°C", "SensorsTemp:", m.getSensorsTemperatures());
        if (m.hasNetwork()) {
            Logger.Infof(TAG, "%-17s %d", "Network RxBytes:", m.getNetwork().getBytesRecv());
            Logger.Infof(TAG, "%-17s %d", "Network TxBytes:", m.getNetwork().getBytesSent());
        }
    }
}
