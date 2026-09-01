# PVP Module (`pvp`)

This package contains gameplay/session mechanics for player-vs-player combat.

## What it contains

- `GameSessionStateMachine`: owns match state (health, hit history, active session) and reacts to packets.
- `GameSessionCoordinator`: lifecycle + identity gate that starts/stops transport based on app and REV/simulator state.
- `PvpEventBus`: RxJava3 singleton for session/gameplay domain events.
- `PvpEvent`: sealed event model for `PvpEventBus`.

## Lifecycle flow

1. Activity `onResume()` -> `GameSessionCoordinator.onHostResumed()`
2. Activity `onPause()` -> `GameSessionCoordinator.onHostPaused()`
3. On connected REV -> `GameSessionCoordinator.onCarConnected(revId, playerName, colorHex, port, allowSelfLoopback)`
4. On simulator mode -> `GameSessionCoordinator.onCarConnected("SIMULATOR:<name>", simulatorName, null, 8888, true)`
5. On disconnect -> `GameSessionCoordinator.onCarDisconnected()`

Listening is active only when host is resumed and a local player identity is available.

## Session flow

- Host starts with `GameSessionCoordinator.startGame(config)`.
- Joiner auto-acknowledges incoming `GAME_START` packets.
- Runtime updates use `registerHitTaken(...)` and `sendGameOver()`.
- If heartbeat gaps exceed the stale threshold, peers emit `RESYNC_REQUEST`; recipients answer with `STATE_SNAPSHOT`.

Heartbeat is scheduled internally by `GameSessionStateMachine` while a session is active.

## Data correctness

- Every packet carries UTC timestamp and ordered packet id.
- Gameplay packets are applied only when newer (timestamp / packet id / hit progression) to filter stale data.
- Sender identity includes `senderId` (stable transport key) and display profile (`senderName`, `senderColorHex`).

## Simulator testing hooks

- `simulator` package now owns simulator dialogs/actions. `AdvancedDrivingFragment` only delegates menu opening.
- `Simulator Events` menu includes:
  - `Emit: Remote Game Start`
  - `Emit: Stale Remote Hit Pair`
  - Existing hit/bump and connection UI-state events.



