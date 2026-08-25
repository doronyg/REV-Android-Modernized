# Robot Module (`robot`)

This package contains the REV callback abstraction layer.

## Contents

- `REVRobotEvent`: sealed event hierarchy mapped from `REVRobotInterface` callbacks.
- `REVRobotEventBroadcaster`: thread-safe Rx bridge implementing `REVRobotInterface`.
- `REVRobotEventBus`: app-level singleton exposing shared `events` and attaching callbacks to connected robots.

