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
3. On connected REV -> `GameSessionCoordinator.onCarConnected(revId)`
4. On simulator mode -> `GameSessionCoordinator.onCarConnected("SIMULATOR:<name>", 8888, true)`
5. On disconnect -> `GameSessionCoordinator.onCarDisconnected()`

Listening is active only when host is resumed and a local player identity is available.

## Session flow

- Host starts with `GameSessionCoordinator.startGame(config)`.
- Joiner accepts with `GameSessionCoordinator.acknowledgeGameStart(startPacket)`.
- Runtime updates use `sendHeartbeat()`, `registerHitTaken(...)`, and `sendGameOver()`.

ACK timeout/retry policies are intentionally left to higher-level app logic.


