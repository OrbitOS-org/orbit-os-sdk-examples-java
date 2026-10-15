# WiFi scan example

List the WiFi interfaces and scan for networks.

An example app for [Orbit OS](https://www.orbit-os.org/?ref=github-sdk-examples-java), built with the [Orbit OS SDK for Java](https://github.com/OrbitOS-org/orbit-os-sdk-java). Part of the [Orbit OS examples for Java](../../README.md).

## Run

Open **this folder** in VS Code with the [Orbit Studio](https://marketplace.visualstudio.com/items?itemName=orbit-os.orbit-studio) extension. You need **Java 17** (a JDK) and a device with Orbit OS in Developer Mode.

1. In the Orbit sidebar, run **Add / Update SDK** and set your device's IP. Orbit Studio downloads the SDK (`v26.0.3`) into `orbit-os-sdk-java/`.
2. Use **Run** to try it live: the example runs on your computer and talks to the device.
3. Use **Build + Deploy** to install the signed `.orb` on the device.

The project builds against the SDK copy in `orbit-os-sdk-java/`, which is not in the repository: Orbit Studio downloads it. Without Orbit Studio, unpack the [SDK release](https://github.com/OrbitOS-org/orbit-os-sdk-java/releases/tag/v26.0.3) into that folder, put the development certificates in `certs/grpc/`, then run `./gradlew :apps:wifi_scan:run --args=<device IP>`.

## Project layout

| Path | What |
|---|---|
| `apps/wifi_scan/src/main/java/org/orbitos/examples/wifiscan/App.java` | the app |
| `apps/wifi_scan/src/main/resources/metadata.json` | manifest: name, version, entry point, permissions |
| `apps/wifi_scan/orb/icon.svg` | launcher icon |
| `apps/wifi_scan/build.gradle`, `proguard-rules.pro` | app build: one JAR with the SDK inside |
| `build.gradle`, `settings.gradle`, `gradlew` | Gradle project, using the local SDK copy in `orbit-os-sdk-java/` |
| `orbit.project.json` | Orbit Studio project settings |

Permissions declared: `SystemService/*`, `WiFiService/*`.
