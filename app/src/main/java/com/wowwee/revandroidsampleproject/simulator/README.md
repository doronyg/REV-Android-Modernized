# Simulator Package (`simulator`)

This package contains simulator-only UI helpers and event injection logic so gameplay fragments stay focused on lifecycle, controls, and PVP events.

## Components

- `SimulatorEventMenu`: builds/shows the simulator dialog and maps menu selections.
- `SimulatorEventDispatcher`: executes simulator actions (local hit/bump callbacks, remote packet injection, UI event simulation).
- `SimulatorBatteryBridge`: emits delayed battery events in simulator mode.
- `SimulatorMenuAction`: typed action enum shared by menu + dispatcher.
- `SimulatorModeController`: handles simulator enable/disable, profile persistence bridge, and identity connection handoff.
- `SimulatorIdentity`: transport/display identity model for simulator sessions.

## Integration points

- `AdvancedDrivingFragment` delegates simulator menu presentation to `SimulatorEventMenu` and action execution to `SimulatorEventDispatcher`.
- `ConnectedRevFragment` delegates simulator battery scheduling to `SimulatorBatteryBridge`.
- `ScanFragment` delegates simulator mode/profile flows to `SimulatorModeController`.


