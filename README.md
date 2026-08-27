# REV Android — Modernized App & SDK

![](Images/REV.png)

This repository is my modernized continuation of the Android app and SDK for the WowWee REV robotic car system.

Original SDK credit: [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git)

> **Why this project exists:** When the original companion apps disappeared from official app stores, many physical REV vehicles were left completely unusable. What started as a weekend effort to restore playability for my family evolved into an ongoing Android, BLE, and networking project.

---

Table of Contents
-----------------

- [What Changed Since the Original SDK](#what-changed-since-the-original-sdk)
- [Engineering Context and Architecture](#engineering-context-and-architecture)
- [Current App Flow](#current-app-flow)
- [Drive Modes](#drive-modes)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Using a Real REV Car](#using-a-real-rev-car)
- [Using Simulator Mode](#using-simulator-mode)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [License and Attribution](#license-and-attribution)
- [Contributing](#contributing)

What Changed Since the Original SDK
-----------------------------------

The original SDK provided the starting point for the REV BLE communication layer, but parts of it required debugging and fixes before the hardware could be reliably discovered and controlled. From there, the project evolved into a broader modernization of the Android application.

Highlights from the modernization work:

- **Modern Android Stack:** Updated the project from its original Android 5.1-era environment to target Android 16 (API 36), using AGP 8.x, JDK 17, and AndroidX.
- **Source Module Integration:** Replaced legacy binary dependencies with source integration through `bluetoothrobotcontrollib_src`.
- **BLE Lifecycle Improvements:** Moved BLE scanning and connection handling out of Fragment/UI lifecycles into dedicated state managers, so vehicle connections are less dependent on individual screens.
- **Simulator Mode:** Added a hardware-independent drive session so the application and controls can be exercised without a physical REV car.
- **Reactive State & Telemetry:** Added RxJava-based event streams for control events and vehicle telemetry.
- **Phone-to-Phone Networking:** Added a lightweight UDP layer for synchronizing game state between phones while each phone maintains its own BLE connection to a REV vehicle.
- **Custom Driving Controls:** Added arcade-style steering controls, throttle interaction, hit/fire controls, and animated steering return-to-center behavior.

Engineering Context and Architecture
------------------------------------

The application builds on the original REV SDK while separating the application code from the underlying vehicle communication layer.

       ┌─────────────────────────────────────────────────────────┐
       │                   Player 1 (Phone A)                    │
       │    [ UI Views ] ──> [ Reactive State / Telemetry ]      │
       └───────────┬─────────────────────────┬───────────────────┘
                   │                         │
              BLE / Control            UDP / Game State
                   │                         │
                   ▼                         ▼
            ┌─────────────┐           ┌─────────────┐
            │ REV Vehicle │           │ Phone B / P2│
            └─────────────┘           └─────────────┘

- **Original SDK:** Based on the original [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git), with fixes to the existing BLE communication code where needed to restore reliable hardware discovery and control.
- **Android Modernization:** Updated the project for current Android tooling, AndroidX, and modern Bluetooth permission and discovery APIs, including Android 12+ `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
- **Application Architecture:** Separated UI code from BLE connection/state handling and drive-session logic.
- **Simulation:** Drive sessions can run without a physical vehicle, allowing the UI and control logic to be exercised independently of BLE hardware.
- **Networking:** UDP is used for phone-to-phone game state while BLE remains responsible for communication with the vehicles.

Current App Flow
----------------

1. **Permissions & Readiness:** The app checks the required runtime permissions and Bluetooth adapter state before starting BLE operations.
2. **Scan & Discovery:** The app performs bounded BLE scans to find nearby REV vehicles, with an option to enter Simulator Mode instead.
3. **Drive Session:** Once a vehicle or simulator session is selected, the app opens the driving controls.
4. **Driving:** Advanced mode is used by default, with the option to switch to Manual mode or other internal testing surfaces.

Drive Modes
-----------

- `Advanced`: Arcade-style steering wheel/yoke, throttle lever, and fire/hit controls.
- `Manual`: Dual-joystick driving layout with fire control.
- `Simulator`: Drive-session simulation without a physical REV vehicle.
- `Experimental Modes`: Internal tools for route tracing and movement command testing.

Requirements
------------

- Android Studio compatible with AGP 8.x.
- JDK 17.
- Target SDK: Android 16 (API 36).
- Minimum SDK: Android 5.1 (API 22).
- Android device with BLE support.
- For physical driving: At least one WowWee REV car.
- For simulator sessions: No physical hardware required.

Quick Start
-----------

1. Clone this repository:

       git clone https://github.com/doronyg/REV-Android-Modernized.git
       cd REV-Android-Modernized

2. Open the project in Android Studio.
3. Allow Gradle sync to complete.
4. Build the application:

       ./gradlew :app:assembleDebug

5. Install and run the `app` module on an Android device.

Using a Real REV Car
--------------------

1. Power on the REV vehicle.
2. Launch the app and complete the required Bluetooth permission prompts.
3. Wait for the scan screen to detect the vehicle, or start discovery manually.
4. Once connected, drive in **Advanced** mode or switch to **Manual** mode.

Using Simulator Mode
--------------------

Simulator Mode allows the application to be used without a physical REV vehicle.

1. Launch the app.
2. On the scan screen, enable **Simulator Mode**.
3. The app creates a simulated drive session without establishing a Bluetooth connection.
4. Use the driving controls normally.
5. Exit Simulator Mode from the scan screen to return to normal BLE operation.

Project Structure
-----------------

- `app/`: Android application module containing the UI, drive controls, UDP networking, and drive-session logic.
- `bluetoothrobotcontrollib_src/`: Source version of the REV Bluetooth communication library, migrated from the original binary dependency.

Roadmap
-------

- Expand multi-car match mechanics and real-time hit scoring.
- Improve BLE reconnection and recovery behavior.
- Continue separating modern application code from the original SDK implementation.

License and Attribution
-----------------------

- This project remains under the **Apache License 2.0**. See [`LICENSE.md`](LICENSE.md).
- Credit to the original SDK and product ecosystem:
  - Original repository: [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git)
  - Product information: <http://www.wowwee.com>
- You are free to build free or paid apps with this SDK. Please clearly differentiate your application from official WowWee branding to avoid user confusion.

Contributing
------------

Issues and pull requests are welcome. If you are building something cool with REV, feel free to open an issue or pull request to share it!