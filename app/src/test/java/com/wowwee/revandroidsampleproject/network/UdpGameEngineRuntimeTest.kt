package com.wowwee.revandroidsampleproject.network

import com.google.gson.Gson
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class UdpGameEngineRuntimeTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        UdpGameEngine.stop()
    }

    @After
    fun tearDown() {
        UdpGameEngine.stop()
    }

    @Test
    fun `drops local packets when self loopback is disabled`() {
        setPrivateField("myRevId", "LOCAL-1")
        setPrivateField("allowSelfLoopback", false)

        val packets = CopyOnWriteArrayList<GameStatePacket>()
        val disposable = NetworkEventBus.events.subscribe { event ->
            if (event is NetworkEvent.PacketReceived) {
                packets.add(event.packet)
            }
        }

        try {
            invokeProcessIncomingPacket(
                gson.toJson(
                    GameStatePacket(
                        senderId = "LOCAL-1",
                        packetId = 10L,
                        timestamp = 1_000L,
                        health = 100,
                        eventType = GameEventType.HEARTBEAT
                    )
                )
            )

            assertTrue(packets.isEmpty())
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `accepts local packets when self loopback is enabled`() {
        setPrivateField("myRevId", "LOCAL-1")
        setPrivateField("allowSelfLoopback", true)

        val packets = CopyOnWriteArrayList<GameStatePacket>()
        val disposable = NetworkEventBus.events.subscribe { event ->
            if (event is NetworkEvent.PacketReceived) {
                packets.add(event.packet)
            }
        }

        try {
            invokeProcessIncomingPacket(
                gson.toJson(
                    GameStatePacket(
                        senderId = "LOCAL-1",
                        packetId = 11L,
                        timestamp = 1_100L,
                        health = 100,
                        eventType = GameEventType.HEARTBEAT
                    )
                )
            )

            assertEquals(1, packets.size)
            assertEquals("LOCAL-1", packets.first().senderId)
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `dedupes by sender instance and only accepts advancing packet ids`() {
        setPrivateField("myRevId", "LOCAL-1")
        setPrivateField("allowSelfLoopback", true)

        val received = CopyOnWriteArrayList<Pair<String, Long>>()
        val disposable = NetworkEventBus.events.subscribe { event ->
            if (event is NetworkEvent.PacketReceived) {
                received.add((event.packet.senderInstanceId ?: "") to event.packet.packetId)
            }
        }

        try {
            val basePacket = GameStatePacket(
                senderId = "REMOTE-1",
                senderInstanceId = "INSTANCE-A",
                packetId = 20L,
                timestamp = 2_000L,
                health = 90,
                eventType = GameEventType.HEARTBEAT
            )
            val duplicatePacket = basePacket.copy(packetId = 20L, timestamp = 2_100L)
            val advancingPacket = basePacket.copy(packetId = 21L, timestamp = 2_200L)
            val differentInstanceSameId = basePacket.copy(senderInstanceId = "INSTANCE-B", packetId = 20L, timestamp = 2_300L)

            invokeProcessIncomingPacket(gson.toJson(basePacket))
            invokeProcessIncomingPacket(gson.toJson(duplicatePacket))
            invokeProcessIncomingPacket(gson.toJson(advancingPacket))
            invokeProcessIncomingPacket(gson.toJson(differentInstanceSameId))

            assertEquals(
                listOf("INSTANCE-A" to 20L, "INSTANCE-A" to 21L, "INSTANCE-B" to 20L),
                received.toList()
            )
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `ignores malformed json without publishing packet events`() {
        setPrivateField("myRevId", "LOCAL-1")
        setPrivateField("allowSelfLoopback", true)

        val packets = CopyOnWriteArrayList<GameStatePacket>()
        val disposable = NetworkEventBus.events.subscribe { event ->
            if (event is NetworkEvent.PacketReceived) {
                packets.add(event.packet)
            }
        }

        try {
            invokeProcessIncomingPacket("not-json")
            assertTrue(packets.isEmpty())
        } finally {
            disposable.dispose()
        }
    }

    private fun invokeProcessIncomingPacket(json: String) {
        val method: Method = UdpGameEngine::class.java.getDeclaredMethod("processIncomingPacket", String::class.java)
        method.isAccessible = true
        method.invoke(UdpGameEngine, json)
    }

    private fun setPrivateField(fieldName: String, value: Any?) {
        val field = UdpGameEngine::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(UdpGameEngine, value)
    }
}
