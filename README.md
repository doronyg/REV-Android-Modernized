REV Android SDK (Modernized & Extended)
=======================================

![](Images/REV.png)

This repository is a modernized, actively maintained Android app and SDK continuation for the WowWee REV robotic car system.

Original SDK credit: [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git)

> **Why this project exists:** When the original companion apps disappeared from app stores, many REV cars became effectively unusable. What started as a weekend effort to restore playability for my family evolved into an ongoing Android/BLE modernization project focused on architecture quality, UX iteration, and reliability.

If you are here to build with REV, welcome. If you are evaluating my work, this repository reflects a pragmatic engineering approach: modern Android tooling, Kotlin-first additions, and clear, evolving app flows.

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

Highlights from the recent modernization work:

- Upgraded build stack (Gradle/AGP), AndroidX migration, and current SDK targets.
- Replaced old binary integration with source module integration for `bluetoothrobotcontrollib_src`.
- Added permissions-first app startup flow and moved BLE scan/connect logic out of fragment lifecycle.
- Added bounded scan sessions, modern BLE discovery flow, and reliability fallbacks.
- Added multiple driving experiences beyond legacy manual flow:
  - Advanced wheel + lever driving mode.
  - Path drawing mode.
  - Experiments mode for repeatable movement command testing.
- Added session-only Simulator Mode so UI and drive flows can be exercised without a car.
- Added kiosk lock architecture for controlled/demo scenarios.
- Refined advanced steering UX with arcade-style wheel/yoke visuals, responsive steering motion, and smooth return-to-center animation.

Engineering Context and Architecture
------------------------------------

If you are reviewing this project as a portfolio-style codebase, this is the practical context:

- **Base platform:** built on top of the original [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git), with continued compatibility for the existing REV BLE control stack.
- **Modernization path:** migrated to AndroidX and AGP 8.x-era tooling, added Kotlin-first flows, and updated runtime permission + BLE discovery handling for modern Android.
- **Engineering direction:** reduced lifecycle coupling (for example, scan/connect orchestration moved out of fragment-only ownership), added simulator-driven development paths, and separated stable user flows from experimental features.

Current App Flow
----------------

1. **Permissions**: the app starts with runtime permission and Bluetooth readiness checks so BLE operations are explicit and predictable.
2. **Scan**: once ready, the app moves into REV discovery/scan, where you either connect to a nearby car or switch into simulator session flow.
3. **Drive**: after entering a session, the app opens driving controls (Advanced by default), with mode switching available for Manual, Path, and Experiments.

This keeps the top-level flow simple while allowing room for iterative UX and architecture changes.

Drive Modes
-----------

- `Manual`: lightweight dual-joystick + fire control.
- `Advanced`: playful steering wheel/yoke + throttle lever + fire.
- `Path`: draw a route and let REV trace it.
- `Experiments`: tune speed/duration/interval for repeatable command testing.

Requirements
------------

- A recent Android Studio version compatible with AGP 8.x.
- JDK 17.
- Android device with BLE support.
- For real driving: at least one physical REV car.
- For simulator sessions: no physical REV required.

Quick Start
-----------

1. Clone this repository.
2. Open it in Android Studio.
3. Let Gradle sync finish.
4. Run the `app` module on a physical Android device.

Example:

```bash
git clone <your-fork-or-repo-url>
cd REV-Android-SDK
./gradlew :app:assembleDebug
```

Using a Real REV Car
--------------------

1. Power on your REV.
2. Open the app and complete permission/Bluetooth prompts.
3. Wait on scan for automatic candidate detection, or use discovery flow when prompted.
4. After connect, drive in Advanced mode or switch to Manual/Path/Experiments.

Using Simulator Mode
--------------------

Use this mode to test navigation and driving UX without a BLE car.

1. Launch the app and complete permission/Bluetooth prompts.
2. On the scan screen, enable **Simulator Mode**.
3. The app opens the driving screen with a nullable REV session.
4. Exit simulator mode from scan when you want to return to live BLE flow.

Note: Simulator Mode is session-only by design and does not emulate physical BLE behavior.

Project Structure
-----------------

- `app/`: Android sample app, modernized flows, and driving modes.
- `bluetoothrobotcontrollib_src/`: REV Bluetooth SDK source module.

Roadmap
-------

- Increase gameplay and "pick up and play" fun.
- Add more car-vs-car game mechanics and match flow ideas.
- Keep improving BLE reliability and compatibility with current Android permission models.
- Continue separating stable features from experimental testing surfaces.

License and Attribution
-----------------------

- This project remains under Apache 2.0. See [`LICENSE.md`](LICENSE.md).
- Credit to the original SDK and product ecosystem:
  - Original repository: [WowWeeLabs/REV-Android-SDK](https://github.com/WowWeeLabs/REV-Android-SDK.git)
  - Product information: <http://www.wowwee.com>
- You are free to build free or paid apps with this SDK. Please clearly differentiate your app from official WowWee branding to avoid user confusion.

Contributing
------------

Issues and pull requests are welcome. If you are building something cool with REV, feel free to open a PR and share it.
