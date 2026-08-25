# Network Module (`network`)

This package provides UI-independent UDP transport and packet models.

## What it contains

- `GameStatePacket`: serializable payload for game event exchange.
- `GameEventType`, `HitRecord`, `GameSessionConfig`, `RevPlayerId`: model types for packet data.
- `UdpGameEngine`: DatagramSocket-based transport with coroutine I/O, deduplication, and optional self-loopback.
- `NetworkEventBus`: RxJava3 singleton for network packet/error events.

## Basic usage

```kotlin
UdpGameEngine.addListener(transportListener)
UdpGameEngine.start(
	myRevId = "AA:BB:CC:DD:EE:FF",
	port = 8888,
	allowSelfLoopback = false
)

UdpGameEngine.sendPacket(packet)

UdpGameEngine.stop()
```

Subscribe to network events via `NetworkEventBus.events`.

Game mechanics, session state, and lifecycle orchestration now live under the `pvp` package.






