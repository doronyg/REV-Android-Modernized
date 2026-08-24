# bluetoothrobotcontrollib_src

Source-integrated version of the REV Bluetooth control library used by this app.

## Origin and modernization

- **Original base:** Adapted from the legacy WowWee REV Android SDK library (`bluetoothrobotcontrollib.jar`), which the upstream project states is available under Apache License 2.0.
- **Why source-integrated:** The legacy binary targets old Android assumptions and required source-level updates for modern Android API levels, permissions, and build tooling.
- **Reconstruction approach:** JAR extracted to `extracted/bluetoothrobotcontrollib/`, then reconstructed using CFR output in `decompiled/bluetoothrobotcontrollib-src/`, then manually integrated under `src/main/java/`.
- **Modernization work:** This module contains project-specific fixes and compatibility changes (transport handling, command dispatch reliability, and modern Android/AGP integration).

## Licensing and attribution

- Keep this module attributed to the original WowWee REV SDK origin plus local modifications in this repository.
- See repository-level `LICENSE.md` and `THIRD_PARTY_NOTICES.md` for license and attribution details.

## Notes

- Decompiled/reconstructed output may require additional cleanup and refactoring over time.
- This module is wired into the app to replace the legacy binary JAR dependency.

